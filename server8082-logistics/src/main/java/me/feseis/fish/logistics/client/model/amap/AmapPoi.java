package me.feseis.fish.logistics.client.model.amap;

import java.math.BigDecimal;

/**
 * 高德 POI 原始结果。坐标取自 pois[].location，GCJ-02。
 */
public record AmapPoi(
        String poiId,
        String poiName,
        String adcode,
        String province,
        String city,
        String district,
        String address,
        BigDecimal lng,
        BigDecimal lat
) {
}
