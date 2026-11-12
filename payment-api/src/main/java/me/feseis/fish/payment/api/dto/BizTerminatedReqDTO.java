package me.feseis.fish.payment.api.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 业务终止事件入参。来源为 logistics.event_outbox(event_type=3)。
 *
 * @param waybillId      关联运单，支付服务按它定位该运单下所有支付单
 * @param bizType        来源业务类型：1运单 2运单取消单
 * @param bizId          来源业务主键
 * @param eventOutboxId  来源事件主键，用于日志追溯
 */
public record BizTerminatedReqDTO(

        @NotBlank(message = "运单主键不能为空")
        @Size(max = 36, message = "运单主键过长")
        String waybillId,

        @NotNull(message = "业务类型不能为空")
        @Min(value = 1, message = "业务类型不合法")
        @Max(value = 5, message = "业务类型不合法")
        Integer bizType,

        @NotBlank(message = "业务主键不能为空")
        @Size(max = 36, message = "业务主键过长")
        String bizId,

        @NotNull(message = "事件主键不能为空")
        Long eventOutboxId
) {
}
