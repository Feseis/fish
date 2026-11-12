package me.feseis.fish.auth.client;

import java.util.List;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.feseis.fish.auth.exp.AuthException;
import me.feseis.fish.auth.exp.ErrorCode;
import me.feseis.fish.contract.ServiceCode;
import me.feseis.fish.sms.api.SmsApiPaths;
import me.feseis.fish.sms.api.dto.PhoneExistsRespDTO;
import me.feseis.fish.sms.api.dto.SmsSendReqDTO;
import me.feseis.fish.sms.api.dto.SmsSendRespDTO;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * 短信服务客户端。验证码统一走内部发送接口，requestId 每次新生成；
 * 发送失败由短信服务落库并返回 result=2，这里统一转成 SMS_SEND_FAILED。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SmsClient {

    private static final String TEMPLATE_LOGIN_CODE = "LOGIN_CODE";
    private static final int RESULT_SUCCESS = 1;

    private final RestClient smsRestClient;

    public void sendLoginCode(String phone, String code) {
        SmsSendReqDTO body = new SmsSendReqDTO(
                UUID.randomUUID().toString(),
                TEMPLATE_LOGIN_CODE,
                phone,
                ServiceCode.AUTH,
                null,
                null,
                List.of(code));
        SmsSendRespDTO resp;
        try {
            resp = smsRestClient.post()
                    .uri(SmsApiPaths.SEND)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(SmsSendRespDTO.class);
        } catch (RestClientException e) {
            log.warn("短信服务调用失败 phone={} reason={}", phone, e.getMessage());
            throw AuthException.of(ErrorCode.SMS_SEND_FAILED);
        }
        if (resp == null || !Integer.valueOf(RESULT_SUCCESS).equals(resp.result())) {
            log.warn("短信投递失败 phone={} reason={}", phone, resp == null ? "空响应" : resp.failReason());
            throw AuthException.of(ErrorCode.SMS_SEND_FAILED);
        }
    }

    /** 查询手机号是否在短信沙盒登记。查询失败按短信服务不可用处理。 */
    public boolean phoneExists(String phone) {
        PhoneExistsRespDTO resp;
        try {
            resp = smsRestClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path(SmsApiPaths.PHONE_EXISTS)
                            .queryParam("phone", phone)
                            .build())
                    .retrieve()
                    .body(PhoneExistsRespDTO.class);
        } catch (RestClientException e) {
            log.warn("短信服务手机号查询失败 phone={} reason={}", phone, e.getMessage());
            throw AuthException.of(ErrorCode.SMS_SEND_FAILED);
        }
        return resp != null && Boolean.TRUE.equals(resp.registered());
    }
}
