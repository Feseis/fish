package me.feseis.fish.logistics.dto.address;

import java.math.BigDecimal;

/**
 * 高德 POI 候选点。坐标取自 pois[].location，GCJ-02。
 */
public record PoiItem(
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
