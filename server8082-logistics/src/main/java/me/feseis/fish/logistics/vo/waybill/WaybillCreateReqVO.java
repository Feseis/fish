package me.feseis.fish.logistics.vo.waybill;

import java.math.BigDecimal;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 下单入参。寄收件都用地址快照，地理部分由前端从 regeo 的候选 POI 原样带入。
 */
public record WaybillCreateReqVO(

        @NotNull(message = "寄件人信息不能为空")
        @Valid
        AddressSnapshot sender,

        @NotNull(message = "收件人信息不能为空")
        @Valid
        AddressSnapshot receiver,

        @NotBlank(message = "物品名称不能为空")
        @Size(max = 64, message = "物品名称过长")
        String itemName,

        @Min(value = 1, message = "物品类别不合法")
        @Max(value = 99, message = "物品类别不合法")
        Integer itemCategoryId,

        @NotNull(message = "计费重量不能为空")
        @DecimalMin(value = "0.001", message = "计费重量必须大于 0")
        @Digits(integer = 7, fraction = 3, message = "计费重量最多 3 位小数")
        BigDecimal itemWeight,

        @DecimalMin(value = "0.00", message = "申报价值不能为负数")
        @Digits(integer = 8, fraction = 2, message = "申报价值最多 2 位小数")
        BigDecimal declaredValue,

        @NotNull(message = "支付方式不能为空")
        @Min(value = 1, message = "支付方式不合法")
        @Max(value = 2, message = "支付方式不合法")
        Integer payMethod,

        /** 1模拟支付 2支付宝沙箱；不传默认 1。仅寄付生效。 */
        @Min(value = 1, message = "支付渠道不合法")
        @Max(value = 2, message = "支付渠道不合法")
        Integer payChannel,

        @Size(max = 255, message = "备注过长")
        String remark
) {

    public record AddressSnapshot(

            @NotBlank(message = "联系人不能为空")
            @Size(max = 32, message = "联系人姓名过长")
            String name,

            @NotBlank(message = "联系电话不能为空")
            @Pattern(regexp = "^1[3-9]\\d{9}$", message = "联系电话格式不正确")
            String phone,

            @NotBlank(message = "行政区划码不能为空")
            @Pattern(regexp = "^\\d{6}$", message = "行政区划码必须是 6 位数字")
            String adcode,

            @NotBlank(message = "省不能为空")
            @Size(max = 32, message = "省名称过长")
            String province,

            @NotBlank(message = "市不能为空")
            @Size(max = 32, message = "市名称过长")
            String city,

            @NotBlank(message = "区县不能为空")
            @Size(max = 32, message = "区县名称过长")
            String district,

            @NotBlank(message = "详细门牌不能为空")
            @Size(max = 200, message = "详细门牌过长")
            String detailAddr,

            @NotBlank(message = "POI 不能为空")
            @Size(max = 64, message = "POI ID 过长")
            String poiId,

            @Size(max = 128, message = "POI 名称过长")
            String poiName,

            @NotNull(message = "经度不能为空")
            @DecimalMin(value = "73.0", message = "经度超出范围")
            @DecimalMax(value = "136.0", message = "经度超出范围")
            @Digits(integer = 3, fraction = 6, message = "经度最多 6 位小数")
            BigDecimal lng,

            @NotNull(message = "纬度不能为空")
            @DecimalMin(value = "3.0", message = "纬度超出范围")
            @DecimalMax(value = "54.0", message = "纬度超出范围")
            @Digits(integer = 2, fraction = 6, message = "纬度最多 6 位小数")
            BigDecimal lat
    ) {
    }
}
