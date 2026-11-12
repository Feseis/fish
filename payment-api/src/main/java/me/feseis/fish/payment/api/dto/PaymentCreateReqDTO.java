package me.feseis.fish.payment.api.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 建单入参。当前只由物流下单后同步调用，属内部接口。
 */
public record PaymentCreateReqDTO(

        @NotNull(message = "业务类型不能为空")
        @Min(value = 1, message = "业务类型不合法")
        @Max(value = 5, message = "业务类型不合法")
        Integer bizType,

        @NotBlank(message = "业务主键不能为空")
        @Size(max = 36, message = "业务主键过长")
        String bizId,

        @NotNull(message = "金额不能为空")
        @DecimalMin(value = "0.01", message = "金额必须大于 0")
        @Digits(integer = 8, fraction = 2, message = "金额最多 2 位小数")
        BigDecimal amount,

        @NotNull(message = "支付渠道不能为空")
        @Min(value = 1, message = "支付渠道不合法")
        @Max(value = 2, message = "支付渠道不合法")
        Integer payChannel,

        /** 1寄付（30 分钟有效）2到付（48 小时有效）。 */
        @NotNull(message = "付款方式不能为空")
        @Min(value = 1, message = "付款方式不合法")
        @Max(value = 2, message = "付款方式不合法")
        Integer payMethod,

        @Size(max = 36, message = "付款账号过长")
        String payerAccountId,

        @Size(max = 20, message = "付款手机号过长")
        String payerPhone
) {
}
