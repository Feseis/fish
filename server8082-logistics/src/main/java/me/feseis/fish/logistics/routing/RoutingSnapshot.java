package me.feseis.fish.logistics.routing;

import java.util.List;
import java.util.Map;

import me.feseis.fish.logistics.mapper.model.RouteRuleRow;
import me.feseis.fish.logistics.mapper.model.SiteAreaRow;
import me.feseis.fish.logistics.mapper.model.SiteRow;
import me.feseis.fish.logistics.mapper.model.TransportLineRow;

/**
 * 路由图快照：一次查库后在内存中完成解析与校验，避免逐跳查库。
 */
public record RoutingSnapshot(
        Map<String, SiteRow> sites,
        List<SiteAreaRow> areas,
        Map<String, TransportLineRow> lines,
        List<RouteRuleRow> rules,
        Map<String, List<RouteRuleRow>> rulesBySite
) {
}
