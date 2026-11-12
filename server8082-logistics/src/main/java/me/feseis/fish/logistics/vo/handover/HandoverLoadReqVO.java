package me.feseis.fish.logistics.vo.handover;

import java.util.List;

import jakarta.validation.constraints.NotEmpty;

/**
 * 装车。可分批提交，已在本交接单内的运单会被幂等跳过。
 */
public record HandoverLoadReqVO(

        @NotEmpty(message = "运单列表不能为空")
        List<String> waybillIds
) {
}
