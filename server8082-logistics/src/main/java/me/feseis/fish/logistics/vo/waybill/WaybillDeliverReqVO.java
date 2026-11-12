package me.feseis.fish.logistics.vo.waybill;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 签收。signType=2 他人代收时，signName 与 signPhone 必填，由服务层校验。
 */
public record WaybillDeliverReqVO(

        @NotNull(message = "签收方式不能为空")
        @Min(value = 1, message = "签收方式不合法")
        @Max(value = 2, message = "签收方式不合法")
        Integer signType,

        @Size(max = 32, message = "签收人姓名过长")
        String signName,

        @Pattern(regexp = "^$|^1[3-9]\\d{9}$", message = "签收人手机号格式不正确")
        String signPhone
) {
}
