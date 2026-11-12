package me.feseis.fish.logistics.support;

import java.util.Optional;

import lombok.RequiredArgsConstructor;
import me.feseis.fish.logistics.exp.BizException;
import me.feseis.fish.logistics.exp.ErrorCode;
import me.feseis.fish.logistics.mapper.StaffMapper;
import me.feseis.fish.logistics.mapper.model.StaffRow;
import org.springframework.stereotype.Component;

/**
 * 物流员工身份判定。员工用同一个 auth 账号登录，靠 logistics_staff 反查是否在职。
 */
@Component
@RequiredArgsConstructor
public class StaffGuard {

    private final StaffMapper staffMapper;

    public Optional<StaffRow> findOnDuty(String accountId) {
        return staffMapper.findOnDutyByAccountId(accountId);
    }

    public boolean isOnDuty(String accountId) {
        return staffMapper.findOnDutyByAccountId(accountId).isPresent();
    }

    public StaffRow requireOnDuty(String accountId) {
        return staffMapper.findOnDutyByAccountId(accountId)
                .orElseThrow(() -> BizException.of(ErrorCode.STAFF_FORBIDDEN));
    }
}
