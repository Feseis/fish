package me.feseis.fish.auth.api.dto;

/**
 * 审核结果。auditOperatorType=1 表示系统审核。
 */
public record AuditDecisionRespDTO(
        boolean approved,
        Integer auditOperatorType,
        String auditOperatorName,
        String remark
) {
}
