package me.feseis.fish.logistics.mapper.model;

import lombok.Data;

/**
 * 网点选项行，供内部列表接口使用，字段与 SiteRow 区分。
 */
@Data
public class SiteOptionRow {
    private String siteId;
    private String siteCode;
    private String siteName;
    private Integer siteType;
    private Integer enabled;
}
