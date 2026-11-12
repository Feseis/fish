package me.feseis.fish.logistics.vo.staff;

import java.time.LocalDateTime;

/**
 * 入职结果。员工身份与角色由同一次申请产生。
 */
public record StaffApplyRespVO(
        String staffId,
        String staffNo,
        String staffName,
        Integer staffRole,
        String siteCode,
        String siteName,
        LocalDateTime appliedAt
) {
}
