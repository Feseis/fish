package me.feseis.fish.payment.api.dto;

/**
 * 业务终止事件处理结果。重复投递时三者均为 0。
 */
public record BizTerminatedRespDTO(
        int closedCount,
        int refundCreatedCount,
        int skippedCount
) {
}
