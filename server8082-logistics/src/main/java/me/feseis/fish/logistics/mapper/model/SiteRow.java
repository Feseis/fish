package me.feseis.fish.logistics.mapper.model;

import lombok.Data;

/**
 * 路由快照用的网点行。
 */
@Data
public class SiteRow {
    private String logisticsSiteId;
    private String siteCode;
    private String siteName;
    private Integer siteType;
    private String parentLogisticsSiteId;
}
