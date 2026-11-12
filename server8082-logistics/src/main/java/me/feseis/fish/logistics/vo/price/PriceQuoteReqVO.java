package me.feseis.fish.logistics.vo.price;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/**
 * 运费试算入参。adcode 为 6 位行政区划码，重量单位 kg，申报价值单位元。
 */
public record PriceQuoteReqVO(

        @NotBlank(message = "寄件地区划码不能为空")
        @Pattern(regexp = "^\\d{6}$", message = "寄件地区划码必须是 6 位数字")
        String senderAdcode,

        @NotBlank(message = "收件地区划码不能为空")
        @Pattern(regexp = "^\\d{6}$", message = "收件地区划码必须是 6 位数字")
        String receiverAdcode,

        @NotNull(message = "计费重量不能为空")
        @DecimalMin(value = "0.001", message = "计费重量必须大于 0")
        @Digits(integer = 7, fraction = 3, message = "计费重量最多 3 位小数")
        BigDecimal itemWeight,

        @DecimalMin(value = "0.00", message = "申报价值不能为负数")
        @Digits(integer = 8, fraction = 2, message = "申报价值最多 2 位小数")
        BigDecimal declaredValue
) {
}
