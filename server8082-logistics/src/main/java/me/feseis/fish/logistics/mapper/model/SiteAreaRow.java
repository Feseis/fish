package me.feseis.fish.logistics.mapper.model;

import lombok.Data;

/**
 * logistics_site_area 查询行：区县 → 营业网点。
 */
@Data
public class SiteAreaRow {
    private String logisticsSiteId;
    private String adcode;
}
