package me.feseis.fish.logistics.routing;

import java.util.List;

/**
 * 路由配置校验结果。只用于日志与排障，不对外暴露。
 */
public record RoutingReport(
        boolean ok,
        int siteCount,
        int areaCount,
        int lineCount,
        int ruleCount,
        int pairTotal,
        int pairReachable,
        List<String> unreachable,
        List<String> brokenChains,
        List<String> cycles,
        List<String> misalignedRules
) {
}
