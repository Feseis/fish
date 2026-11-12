package me.feseis.fish.auth.config.properties;

import java.time.Duration;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * 认证服务运行参数。绑定 application.yml 的 auth 子树，缺失键在启动期即失败。
 */
@Validated
@ConfigurationProperties(prefix = "auth")
public record AuthProperties(@Valid Token token, @Valid Cookie cookie, @Valid Sms sms) {

    public record Token(
            @NotNull Duration accessTtl,
            @NotNull Duration refreshTtl
    ) {
    }

    public record Cookie(@NotNull Boolean secure) {
    }

    public record Sms(
            @NotBlank String baseUrl,
            @NotNull Duration codeTtl,
            @NotNull Duration resendInterval
    ) {
    }
}
