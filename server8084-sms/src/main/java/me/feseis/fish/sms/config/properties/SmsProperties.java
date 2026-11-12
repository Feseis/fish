package me.feseis.fish.sms.config.properties;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * 短信服务运行参数。绑定 application.yml 的 sms 子树，缺失键在启动期即失败。
 */
@Validated
@ConfigurationProperties(prefix = "sms")
public record SmsProperties(@Valid @NotNull Pbox pbox) {

    public record Pbox(
            @NotBlank String baseUrl,
            @NotBlank String senderPhone,
            @NotBlank String apiCode
    ) {
    }
}
