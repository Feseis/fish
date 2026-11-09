package me.feseis.fish.logistics.dto.address;

import java.util.List;

/**
 * 逆地理编码出参：行政区 + 候选 POI。只返回给前端选点，不落库。
 */
public record RegeoResponse(
        String formattedAddress,
        String province,
        String city,
        String district,
        String adcode,
        List<PoiItem> pois
) {
}
