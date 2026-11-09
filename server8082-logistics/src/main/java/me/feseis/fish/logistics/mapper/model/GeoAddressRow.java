package me.feseis.fish.logistics.mapper.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import lombok.Data;

/**
 * geo_address 查询行。
 */
@Data
public class GeoAddressRow {
    private String geoAddressId;
    private String poiId;
    private String poiName;
    private String adcode;
    private String province;
    private String city;
    private String district;
    private BigDecimal lng;
    private BigDecimal lat;
}
