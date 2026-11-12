package me.feseis.fish.logistics.routing;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;

import lombok.RequiredArgsConstructor;
import me.feseis.fish.logistics.mapper.RoutingMapper;
import me.feseis.fish.logistics.mapper.model.RouteRuleRow;
import me.feseis.fish.logistics.mapper.model.SiteRow;
import me.feseis.fish.logistics.mapper.model.TransportLineRow;
import org.springframework.stereotype.Service;

/**
 * 逐跳路由解析。
 *
 * <p>取下一跳的顺序：
 * <ol>
 *   <li>先查 route_rule，按目的 adcode 最长前缀命中；用于干线与例外规则。</li>
 *   <li>未命中且当前是营业网点时，走「到上级转运中心」的支线；这就是层级默认。</li>
 *   <li>未命中且当前是转运中心时，视为断链。</li>
 * </ol>
 */
@Service
@RequiredArgsConstructor
public class RouteResolver {

    private final RoutingMapper routingMapper;

    public RoutingSnapshot loadSnapshot() {
        var sites = new LinkedHashMap<String, SiteRow>();
        for (SiteRow site : routingMapper.listEnabledRoutingSites()) {
            sites.put(site.getLogisticsSiteId(), site);
        }

        var lines = new LinkedHashMap<String, TransportLineRow>();
        for (TransportLineRow line : routingMapper.listEnabledLines()) {
            lines.put(line.getTransportLineId(), line);
        }

        List<RouteRuleRow> rules = routingMapper.listEnabledRules();
        var rulesBySite = new HashMap<String, List<RouteRuleRow>>();
        for (RouteRuleRow rule : rules) {
            rulesBySite.computeIfAbsent(rule.getCurrentSiteId(), key -> new ArrayList<>()).add(rule);
        }

        return new RoutingSnapshot(sites, routingMapper.listSiteAreas(), lines, rules, rulesBySite);
    }

    /** 返回下一跳线路；无法确定时返回 empty，由调用方按断链处理。 */
    public Optional<TransportLineRow> next(RoutingSnapshot snapshot, String currentSiteId, String destAdcode) {
        RouteRuleRow matched = longestPrefixMatch(
                snapshot.rulesBySite().getOrDefault(currentSiteId, List.of()), destAdcode);
        if (matched != null) {
            return Optional.ofNullable(snapshot.lines().get(matched.getTransportLineId()));
        }

        SiteRow current = snapshot.sites().get(currentSiteId);
        if (current != null && Integer.valueOf(1).equals(current.getSiteType())
                && current.getParentLogisticsSiteId() != null) {
            for (TransportLineRow line : snapshot.lines().values()) {
                if (currentSiteId.equals(line.getOriginSiteId())
                        && current.getParentLogisticsSiteId().equals(line.getDestSiteId())) {
                    return Optional.of(line);
                }
            }
        }
        return Optional.empty();
    }

    private RouteRuleRow longestPrefixMatch(List<RouteRuleRow> rules, String destAdcode) {
        if (destAdcode == null) {
            return null;
        }
        RouteRuleRow best = null;
        for (RouteRuleRow rule : rules) {
            String prefix = rule.getDestAdcodePrefix();
            if (prefix == null || prefix.isEmpty() || !destAdcode.startsWith(prefix)) {
                continue;
            }
            if (best == null || prefix.length() > best.getDestAdcodePrefix().length()) {
                best = rule;
            }
        }
        return best;
    }
}
