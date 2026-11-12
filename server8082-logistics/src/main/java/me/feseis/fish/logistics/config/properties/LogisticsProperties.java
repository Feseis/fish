package me.feseis.fish.logistics.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 物流服务自有配置。高德走 Web 服务 Key，只存在服务端；auth 用于令牌校验；payment 用于寄付建单。
 */
@ConfigurationProperties(prefix = "logistics")
public record LogisticsProperties(Amap amap, Auth auth, Payment payment) {

    public record Amap(String baseUrl, String key) {
    }

    public record Auth(String baseUrl) {
    }

    public record Payment(String baseUrl) {
    }
}
