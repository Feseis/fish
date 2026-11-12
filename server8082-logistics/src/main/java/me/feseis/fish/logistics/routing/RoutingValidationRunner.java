package me.feseis.fish.logistics.routing;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.feseis.fish.logistics.routing.RoutingReport;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * 启动时校验路由配置异常。失败只告警，不阻止启动：种子数据未导入时服务仍应可用。
 * 可用 logistics.routing.validate-on-startup=false 关闭，避免每次启动都跑全量校验。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RoutingValidationRunner implements ApplicationRunner {

    @Value("${logistics.routing.validate-on-startup:true}")
    private boolean validateOnStartup;

    private final RoutingValidator routingValidator;

    @Override
    public void run(ApplicationArguments args) {
        if (!validateOnStartup) {
            log.info("路由配置校验已关闭");
            return;
        }

        RoutingReport report;
        try {
            report = routingValidator.validate();
        } catch (RuntimeException e) {
            log.warn("路由配置校验跳过：{}", e.getMessage());
            return;
        }

        if (report.siteCount() == 0 || report.areaCount() == 0) {
            log.info("路由数据为空，跳过校验");
            return;
        }

        if (report.ok()) {
            log.info("routing validation passed: sites={} areas={} lines={} rules={} pairs={}/{}",
                    report.siteCount(), report.areaCount(), report.lineCount(), report.ruleCount(),
                    report.pairReachable(), report.pairTotal());
            return;
        }

        log.warn("routing validation failed: pairs={}/{}, unreachable={}, broken={}, cycles={}, misaligned={}",
                report.pairReachable(), report.pairTotal(),
                report.unreachable().size(), report.brokenChains().size(),
                report.cycles().size(), report.misalignedRules().size());
        report.unreachable().forEach(item -> log.warn("  不可达: {}", item));
        report.brokenChains().forEach(item -> log.warn("  断链: {}", item));
        report.cycles().forEach(item -> log.warn("  成环: {}", item));
        report.misalignedRules().forEach(item -> log.warn("  规则错位: {}", item));
    }
}
