package me.feseis.fish.logistics.mapper.model;

import lombok.Data;

/**
 * transport_line 查询行。
 */
@Data
public class TransportLineRow {
    private String transportLineId;
    private String lineCode;
    private String originSiteId;
    private String destSiteId;
    private Integer lineType;
    private Integer transitHours;
}
