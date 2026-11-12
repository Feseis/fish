package me.feseis.fish.logistics.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.feseis.fish.auth.api.AuthApiPaths;
import me.feseis.fish.auth.api.dto.AccountLookupRespDTO;
import me.feseis.fish.auth.api.dto.AuditDecisionRespDTO;
import me.feseis.fish.auth.api.dto.RoleGrantReqDTO;
import me.feseis.fish.auth.api.dto.VerifyRespDTO;
import me.feseis.fish.auth.api.dto.WaybillCancelAuditReqDTO;
import me.feseis.fish.contract.AuthHeaders;
import me.feseis.fish.logistics.exp.BizException;
import me.feseis.fish.logistics.exp.ErrorCode;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * 调 auth 的内部接口换取调用者身份、发起审核。
 * 不在本地解析令牌，避免与 auth 的签发规则重复实现。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AuthClient {

    private final RestClient authRestClient;

    public String verify(String accessToken) {
        VerifyRespDTO response;
        try {
            response = authRestClient.post()
                    .uri(AuthApiPaths.VERIFY_INTERNAL)
                    .header(AuthHeaders.AUTHORIZATION, AuthHeaders.BEARER_PREFIX + accessToken)
                    .retrieve()
                    .onStatus(HttpStatusCode::is4xxClientError,
                            (request, resp) -> {
                                throw BizException.of(ErrorCode.UNAUTHORIZED);
                            })
                    .body(VerifyRespDTO.class);
        } catch (RestClientException e) {
            log.warn("auth 校验调用失败: {}", e.getMessage());
            throw BizException.of(ErrorCode.AUTH_UNAVAILABLE);
        }

        if (response == null || response.accountId() == null || response.accountId().isBlank()) {
            throw BizException.of(ErrorCode.UNAUTHORIZED);
        }
        return response.accountId();
    }

    /**
     * 调 auth 的占位审核接口。当前 auth 侧固定返回通过，此处仍做防御：非 2xx 或 approved=false 一律视为不可用。
     */
    public void auditWaybillCancel(String waybillCancelId,
                                   String waybillId,
                                   Integer cancelReasonId,
                                   Integer statusBefore,
                                   Integer applyOperatorType,
                                   String applyOperatorId) {
        AuditDecisionRespDTO decision;
        try {
            decision = authRestClient.post()
                    .uri(AuthApiPaths.AUDIT_WAYBILL_CANCEL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new WaybillCancelAuditReqDTO(waybillCancelId, waybillId, cancelReasonId,
                            statusBefore, applyOperatorType, applyOperatorId))
                    .retrieve()
                    .onStatus(HttpStatusCode::isError,
                            (request, resp) -> {
                                throw BizException.of(ErrorCode.AUDIT_UNAVAILABLE);
                            })
                    .body(AuditDecisionRespDTO.class);
        } catch (RestClientException e) {
            log.warn("审核占位接口调用失败: {}", e.getMessage());
            throw BizException.of(ErrorCode.AUDIT_UNAVAILABLE);
        }
        if (decision == null || !decision.approved()) {
            throw BizException.of(ErrorCode.AUDIT_UNAVAILABLE);
        }
    }

    /** 按手机号取账号与权威姓名。账号不存在映射 404，其余异常视为 auth 不可用。 */
    public AccountLookupRespDTO findAccountByPhone(String phone) {
        try {
            AccountLookupRespDTO response = authRestClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path(AuthApiPaths.ACCOUNT_BY_PHONE)
                            .queryParam("phone", phone)
                            .build())
                    .retrieve()
                    .onStatus(status -> status.value() == 404,
                            (request, resp) -> {
                                throw BizException.of(ErrorCode.ACCOUNT_NOT_FOUND);
                            })
                    .onStatus(HttpStatusCode::isError,
                            (request, resp) -> {
                                throw BizException.of(ErrorCode.AUTH_UNAVAILABLE);
                            })
                    .body(AccountLookupRespDTO.class);
            if (response == null || response.accountId() == null) {
                throw BizException.of(ErrorCode.AUTH_UNAVAILABLE);
            }
            return response;
        } catch (RestClientException e) {
            log.warn("auth 账号查询失败 phone={}: {}", phone, e.getMessage());
            throw BizException.of(ErrorCode.AUTH_UNAVAILABLE);
        }
    }

    /** 授予角色。幂等，已授予仍返回 2xx。 */
    public void grantRole(String accountId, int roleId) {
        try {
            authRestClient.post()
                    .uri(AuthApiPaths.GRANT_ROLE_INTERNAL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new RoleGrantReqDTO(accountId, roleId))
                    .retrieve()
                    .onStatus(HttpStatusCode::isError,
                            (request, resp) -> {
                                throw BizException.of(ErrorCode.AUTH_UNAVAILABLE);
                            })
                    .toBodilessEntity();
        } catch (RestClientException e) {
            log.warn("auth 授予角色失败 accountId={} roleId={}: {}", accountId, roleId, e.getMessage());
            throw BizException.of(ErrorCode.AUTH_UNAVAILABLE);
        }
    }
}
