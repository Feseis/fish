package me.feseis.fish.logistics.vo.address;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 保存地址簿条目。地理部分由前端从 regeo 的候选 POI 原样带入。
 */
public record UserAddressSaveReqVO(

        @NotBlank(message = "POI 不能为空")
        @Size(max = 64, message = "POI ID 过长")
        String poiId,

        @Size(max = 128, message = "POI 名称过长")
        String poiName,

        @NotBlank(message = "行政区划码不能为空")
        @Pattern(regexp = "^\\d{6}$", message = "行政区划码必须是 6 位数字")
        String adcode,

        @Size(max = 32, message = "省名称过长")
        String province,

        @Size(max = 32, message = "市名称过长")
        String city,

        @Size(max = 32, message = "区县名称过长")
        String district,

        @NotNull(message = "经度不能为空")
        @DecimalMin(value = "73.0", message = "经度超出范围")
        @DecimalMax(value = "136.0", message = "经度超出范围")
        @Digits(integer = 3, fraction = 6, message = "经度最多 6 位小数")
        BigDecimal lng,

        @NotNull(message = "纬度不能为空")
        @DecimalMin(value = "3.0", message = "纬度超出范围")
        @DecimalMax(value = "54.0", message = "纬度超出范围")
        @Digits(integer = 2, fraction = 6, message = "纬度最多 6 位小数")
        BigDecimal lat,

        @NotBlank(message = "联系人不能为空")
        @Size(max = 32, message = "联系人姓名过长")
        String contactName,

        @NotBlank(message = "联系电话不能为空")
        @Pattern(regexp = "^1[3-9]\\d{9}$", message = "联系电话格式不正确")
        String contactPhone,

        @NotBlank(message = "详细门牌不能为空")
        @Size(max = 200, message = "详细门牌过长")
        String detailAddr,

        @Size(max = 16, message = "标签过长")
        String tag
) {
}
