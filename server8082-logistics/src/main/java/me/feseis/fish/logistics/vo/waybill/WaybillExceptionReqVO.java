package me.feseis.fish.logistics.vo.waybill;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 异常标记入参。异常原因取自 exception_reason 字典。
 */
public record WaybillExceptionReqVO(

        @NotNull(message = "异常原因不能为空")
        @Min(value = 1, message = "异常原因不合法")
        Integer exceptionReasonId,

        @Size(max = 255, message = "补充说明过长")
        String exceptionRemark
) {
}
