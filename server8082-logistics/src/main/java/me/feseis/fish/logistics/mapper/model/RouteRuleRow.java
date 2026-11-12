package me.feseis.fish.logistics.mapper.model;

import lombok.Data;

/**
 * route_rule 查询行。
 */
@Data
public class RouteRuleRow {
    private String routeRuleId;
    private String currentSiteId;
    private String destAdcodePrefix;
    private String transportLineId;
}
