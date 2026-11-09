package me.feseis.fish.logistics.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.feseis.fish.contract.AuthHeaders;
import me.feseis.fish.logistics.exp.BizException;
import me.feseis.fish.logistics.exp.ErrorCode;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * 调 auth 的 /auth/verify 换取调用者身份。不在本地解析令牌，避免与 auth 的签发规则重复实现。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AuthClient {

    private final RestClient authRestClient;

    public String verify(String accessToken) {
        AuthVerifyResponse response;
        try {
            response = authRestClient.post()
                    .uri("/auth/verify")
                    .header(AuthHeaders.AUTHORIZATION, AuthHeaders.BEARER_PREFIX + accessToken)
                    .retrieve()
                    .onStatus(HttpStatusCode::is4xxClientError,
                            (request, resp) -> {
                                throw BizException.of(ErrorCode.UNAUTHORIZED);
                            })
                    .body(AuthVerifyResponse.class);
        } catch (RestClientException e) {
            log.warn("auth 校验调用失败: {}", e.getMessage());
            throw BizException.of(ErrorCode.AUTH_UNAVAILABLE);
        }

        if (response == null || response.accountId() == null || response.accountId().isBlank()) {
            throw BizException.of(ErrorCode.UNAUTHORIZED);
        }
        return response.accountId();
    }

    public record AuthVerifyResponse(String sessionId, String accountId, java.util.List<Integer> roles) {
    }
}
