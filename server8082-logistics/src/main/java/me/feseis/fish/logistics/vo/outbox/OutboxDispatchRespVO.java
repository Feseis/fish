package me.feseis.fish.logistics.vo.outbox;

import java.util.List;

/**
 * 手动投递结果。details 逐条给出事件去向，便于排障。
 */
public record OutboxDispatchRespVO(
        int scanned,
        int delivered,
        int retried,
        int abandoned,
        List<Item> details
) {
    public record Item(Long eventId, Integer status, String error) {
    }
}
