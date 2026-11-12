package me.feseis.fish.logistics.support;

import me.feseis.fish.logistics.exp.BizException;
import me.feseis.fish.logistics.exp.ErrorCode;
import org.springframework.stereotype.Component;

/**
 * 状态跳转守卫。只做「当前状态是否在允许集合内」判断，具体目标状态由调用方计算。
 */
@Component
public class WaybillStatusGuard {

    public void require(int actual, int... allowed) {
        for (int candidate : allowed) {
            if (candidate == actual) {
                return;
            }
        }
        throw BizException.of(ErrorCode.WAYBILL_STATUS_INVALID);
    }
}
