package me.feseis.fish.logistics.client.model.amap;

import java.util.List;

/**
 * 高德逆地理编码原始结果。只表示第三方返回结构，与对外 VO 解耦。
 */
public record AmapRegeoResult(
        String formattedAddress,
        String province,
        String city,
        String district,
        String adcode,
        List<AmapPoi> pois
) {
}
