package me.feseis.fish.logistics.vo.staff;

/**
 * 网点选项。不过滤启用状态与类型，由调用方自行判断。
 */
public record SiteOptionVO(
        String siteId,
        String siteCode,
        String siteName,
        Integer siteType,
        Integer enabled
) {
}
