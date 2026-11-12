package me.feseis.fish.auth.api.dto;

/**
 * 员工入职审核入参。当前占位实现不读取字段。
 */
public record StaffApplyAuditReqDTO(
        String staffApplyId,
        String authAccountId,
        String staffName
) {
}
