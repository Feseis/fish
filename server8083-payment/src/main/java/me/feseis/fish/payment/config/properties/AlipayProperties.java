package me.feseis.fish.payment.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 支付宝沙箱配置。privateKey 是应用私钥（PKCS8），publicKey 是支付宝公钥，两者不是一对。
 */
@ConfigurationProperties(prefix = "payment.alipay")
public record AlipayProperties(
        boolean enabled,
        String appId,
        String gateway,
        String privateKey,
        String publicKey,
        String notifyUrl
) {

    public boolean isConfigured() {
        return enabled && hasText(appId) && hasText(gateway) && hasText(privateKey) && hasText(publicKey);
    }

    public boolean hasNotifyUrl() {
        return hasText(notifyUrl);
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
