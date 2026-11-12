package me.feseis.fish.auth.api.dto;

/**
 * 运单取消审核入参。当前占位实现不读取字段，仅保留结构供将来替换为真实人工审核。
 */
public record WaybillCancelAuditReqDTO(
        String waybillCancelId,
        String waybillId,
        Integer cancelReasonId,
        Integer statusBefore,
        Integer applyOperatorType,
        String applyOperatorId
) {
}
