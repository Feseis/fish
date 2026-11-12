package me.feseis.fish.logistics.controller;

import java.util.List;

import lombok.RequiredArgsConstructor;
import me.feseis.fish.logistics.mapper.SiteMapper;
import me.feseis.fish.logistics.vo.staff.SiteOptionVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 内部网点列表。当前只供发证脚本等工具使用，不做任何过滤。
 */
@RestController
@RequiredArgsConstructor
public class InternalSiteController {

    private static final String SITES = "/logistics/internal/sites";

    private final SiteMapper siteMapper;

    @GetMapping(SITES)
    public List<SiteOptionVO> list() {
        return siteMapper.listAll().stream()
                .map(row -> new SiteOptionVO(row.getSiteId(), row.getSiteCode(), row.getSiteName(),
                        row.getSiteType(), row.getEnabled()))
                .toList();
    }
}
