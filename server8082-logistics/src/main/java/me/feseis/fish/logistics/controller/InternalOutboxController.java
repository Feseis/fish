package me.feseis.fish.logistics.controller;

import lombok.RequiredArgsConstructor;
import me.feseis.fish.logistics.service.OutboxDispatchService;
import me.feseis.fish.logistics.vo.outbox.OutboxDispatchRespVO;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 跨库事件手动投递。定时任务接入后本接口保留，用于人工补投。
 */
@RestController
@RequiredArgsConstructor
public class InternalOutboxController {

    private static final String DISPATCH = "/logistics/internal/outbox/dispatch";
    private static final int MAX_LIMIT = 100;

    private final OutboxDispatchService outboxDispatchService;

    @PostMapping(DISPATCH)
    public OutboxDispatchRespVO dispatch(@RequestParam(defaultValue = "50") int limit) {
        int safeLimit = Math.min(Math.max(limit, 1), MAX_LIMIT);
        return outboxDispatchService.dispatch(safeLimit);
    }
}
