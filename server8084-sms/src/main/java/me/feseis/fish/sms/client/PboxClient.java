package me.feseis.fish.sms.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.feseis.fish.sms.config.properties.SmsProperties;
import me.feseis.fish.sms.exp.BizException;
import me.feseis.fish.sms.exp.ErrorCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * 走 fish-pbox 的短信投递：POST /sms/send-api，senderPhone 与 apiCode 配对识别发送方。
 * 只负责投递，不落库；apiCode 不入日志。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PboxClient {

    private final RestClient pboxRestClient;
    private final SmsProperties props;

    public void send(String phone, String content, String messageType) {
        PboxSendRequest body = new PboxSendRequest(
                props.pbox().senderPhone(),
                props.pbox().apiCode(),
                phone,
                content,
                messageType);
        try {
            pboxRestClient.post()
                    .uri("/sms/send-api")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException e) {
            log.warn("pbox 投递失败 phone={} reason={}", phone, e.getMessage());
            throw BizException.of(ErrorCode.SMS_SEND_FAILED, e.getMessage());
        }
    }

    /** 查询手机号是否在沙盒登记。pbox 返回 200 + registered，不区分 404。 */
    public boolean exists(String phone) {
        PboxPhoneExistsResponse response;
        try {
            response = pboxRestClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/sms/phones/exists")
                            .queryParam("phone", phone)
                            .build())
                    .retrieve()
                    .body(PboxPhoneExistsResponse.class);
        } catch (RestClientException e) {
            log.warn("pbox 手机号查询失败 phone={} reason={}", phone, e.getMessage());
            throw BizException.of(ErrorCode.SMS_SEND_FAILED, e.getMessage());
        }
        return response != null && Boolean.TRUE.equals(response.registered());
    }

    public record PboxSendRequest(
            String senderPhone,
            String apiCode,
            String receiverPhone,
            String content,
            String messageType
    ) {
    }

    public record PboxPhoneExistsResponse(
            String phone,
            Boolean registered
    ) {
    }
}
