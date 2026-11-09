package me.feseis.fish.auth.sender;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.feseis.fish.auth.config.properties.AuthProperties;
import me.feseis.fish.auth.exp.AuthException;
import me.feseis.fish.auth.exp.ErrorCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * 走 fish-pbox 的验证码投递：POST /sms/send，senderPhone 与 apiCode 配对识别特殊发送方。
 * 只落库不真实下发，手机端按 receiverPhone 轮询取码。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PboxSmsSender implements SmsSender {

    private static final String MESSAGE_TYPE_CODE = "CODE";

    private final RestClient pboxRestClient;
    private final AuthProperties props;

    @Override
    public void send(String phone, String code) {
        String msg = String.format("【飞鱼速递】验证码提醒：尊敬的用户，您正在使用验证码登录短链条，验证码位 %s",code);
        PboxSendRequest body = new PboxSendRequest(
                props.sms().pbox().senderPhone(),
                props.sms().pbox().apiCode(),
                phone,
                code,
                MESSAGE_TYPE_CODE);
        try {
            pboxRestClient.post()
                    .uri("/sms/send-api")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException e) {
            // 非 2xx、超时、连接拒绝都落在这里；apiCode 不入日志
            log.warn("pbox 投递失败 phone={} reason={}", phone, e.getMessage());
            throw AuthException.of(ErrorCode.SMS_SEND_FAILED);
        }
    }

    public record PboxSendRequest(
            String senderPhone,
            String apiCode,
            String receiverPhone,
            String content,
            String messageType
    ) {
    }
}
