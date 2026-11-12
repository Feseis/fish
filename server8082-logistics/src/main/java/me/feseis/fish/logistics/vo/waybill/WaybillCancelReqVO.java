package me.feseis.fish.logistics.vo.waybill;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 取消申请入参。取消原因取自 cancel_reason 字典。
 */
public record WaybillCancelReqVO(

        @NotNull(message = "取消原因不能为空")
        @Min(value = 1, message = "取消原因不合法")
        Integer cancelReasonId,

        @Size(max = 255, message = "补充说明过长")
        String cancelRemark
) {
}
