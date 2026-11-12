package me.feseis.fish.logistics.routing;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import lombok.RequiredArgsConstructor;
import me.feseis.fish.logistics.routing.RoutingReport;
import me.feseis.fish.logistics.mapper.model.RouteRuleRow;
import me.feseis.fish.logistics.mapper.model.SiteAreaRow;
import me.feseis.fish.logistics.mapper.model.SiteRow;
import me.feseis.fish.logistics.mapper.model.TransportLineRow;
import org.springframework.stereotype.Service;

/**
 * 路由配置校验：可达性、断链、环、规则与线路对齐。
 *
 * <p>可达性矩阵为「每个营业网点 → 每个已覆盖区县的归属网点」。线路双向各配一条，
 * 因此该矩阵同时覆盖正向揽收到派送、反向派送到揽收两种口径。
 */
@Service
@RequiredArgsConstructor
public class RoutingValidator {

    private static final int MAX_HOPS = 10;

    private final RouteResolver routeResolver;

    public RoutingReport validate() {
        RoutingSnapshot snapshot = routeResolver.loadSnapshot();

        List<String> misaligned = checkRuleAlignment(snapshot);
        List<String> unreachable = new ArrayList<>();
        List<String> brokenChains = new ArrayList<>();
        List<String> cycles = new ArrayList<>();

        List<SiteRow> sites = new ArrayList<>(snapshot.sites().values());
        int pairTotal = 0;
        int pairReachable = 0;

        for (SiteRow site : sites) {
            // 只有营业网点承担揽收与派送，转运中心不参与可达矩阵
            if (!Integer.valueOf(1).equals(site.getSiteType())) {
                continue;
            }
            for (SiteAreaRow area : snapshot.areas()) {
                pairTotal++;
                Probe probe = probe(snapshot, site.getLogisticsSiteId(), area.getAdcode(),
                        area.getLogisticsSiteId());
                if (probe.reachable()) {
                    pairReachable++;
                    continue;
                }
                unreachable.add(site.getSiteCode() + " -> " + area.getAdcode()
                        + "（目标 " + area.getLogisticsSiteId() + "）");
                brokenChains.addAll(probe.brokenChains());
                cycles.addAll(probe.cycles());
            }
        }

        boolean ok = unreachable.isEmpty() && misaligned.isEmpty() && cycles.isEmpty();

        return new RoutingReport(
                ok,
                sites.size(),
                snapshot.areas().size(),
                snapshot.lines().size(),
                snapshot.rules().size(),
                pairTotal,
                pairReachable,
                unreachable,
                new ArrayList<>(new LinkedHashSet<>(brokenChains)),
                new ArrayList<>(new LinkedHashSet<>(cycles)),
                misaligned);
    }

    private List<String> checkRuleAlignment(RoutingSnapshot snapshot) {
        List<String> problems = new ArrayList<>();
        for (RouteRuleRow rule : snapshot.rules()) {
            TransportLineRow line = snapshot.lines().get(rule.getTransportLineId());
            if (line == null) {
                problems.add("规则 " + rule.getRouteRuleId() + " 指向的线路不存在或已停用");
                continue;
            }
            if (!line.getOriginSiteId().equals(rule.getCurrentSiteId())) {
                problems.add("规则 " + rule.getRouteRuleId() + " 的当前网点 " + rule.getCurrentSiteId()
                        + " 不是线路 " + line.getLineCode() + " 的起点");
            }
        }
        return problems;
    }

    private Probe probe(RoutingSnapshot snapshot, String startSiteId, String destAdcode, String targetSiteId) {
        if (startSiteId.equals(targetSiteId)) {
            return Probe.REACHED;
        }

        Set<String> visited = new HashSet<>();
        Deque<String> queue = new ArrayDeque<>();
        queue.add(startSiteId);
        visited.add(startSiteId);

        List<String> broken = new ArrayList<>();
        List<String> cycles = new ArrayList<>();

        for (int hop = 0; hop < MAX_HOPS && !queue.isEmpty(); hop++) {
            int layerSize = queue.size();
            for (int i = 0; i < layerSize; i++) {
                String current = queue.poll();
                Optional<TransportLineRow> next = routeResolver.next(snapshot, current, destAdcode);
                if (next.isEmpty()) {
                    broken.add("断链：" + current + " 在目的 " + destAdcode + " 下无下一跳");
                    continue;
                }
                String nextSite = next.get().getDestSiteId();
                if (nextSite.equals(targetSiteId)) {
                    return Probe.REACHED;
                }
                if (!visited.add(nextSite)) {
                    cycles.add("路由环：" + current + " -> " + nextSite + " 重复访问");
                    continue;
                }
                queue.add(nextSite);
            }
        }

        return new Probe(false, broken, cycles);
    }

    private record Probe(boolean reachable, List<String> brokenChains, List<String> cycles) {
        static final Probe REACHED = new Probe(true, List.of(), List.of());
    }
}
