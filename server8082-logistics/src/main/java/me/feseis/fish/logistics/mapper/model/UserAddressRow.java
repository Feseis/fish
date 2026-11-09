package me.feseis.fish.logistics.mapper.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import lombok.Data;

/**
 * user_address 与 geo_address 联查行，用于地址簿列表与详情。
 */
@Data
public class UserAddressRow {
    private String userAddressId;
    private String authAccountId;
    private String geoAddressId;
    private String contactName;
    private String contactPhone;
    private String detailAddr;
    private String tag;
    private LocalDateTime createdAt;
    private LocalDateTime deletedAt;

    private String poiId;
    private String poiName;
    private String adcode;
    private String province;
    private String city;
    private String district;
    private BigDecimal lng;
    private BigDecimal lat;
}
