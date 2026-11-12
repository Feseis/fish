package me.feseis.fish.logistics.vo.staff;

import java.time.LocalDateTime;

/**
 * 签发培训凭证出参。凭证状态只有有效与失效，有效期到即失效。
 */
public record StaffTrainingIssueRespVO(
        String staffTrainingId,
        String voucherCode,
        LocalDateTime expireAt,
        Integer staffRole,
        String siteCode,
        String siteName,
        String traineeName,
        String traineeAccountId
) {
}
