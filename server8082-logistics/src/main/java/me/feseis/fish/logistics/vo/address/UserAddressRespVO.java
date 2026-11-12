package me.feseis.fish.logistics.vo.address;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 地址簿条目出参，地理信息来自 geo_address 缓存。
 */
public record UserAddressRespVO(
        String userAddressId,
        String poiId,
        String poiName,
        String adcode,
        String province,
        String city,
        String district,
        BigDecimal lng,
        BigDecimal lat,
        String contactName,
        String contactPhone,
        String detailAddr,
        String tag,
        LocalDateTime createdAt
) {
}
