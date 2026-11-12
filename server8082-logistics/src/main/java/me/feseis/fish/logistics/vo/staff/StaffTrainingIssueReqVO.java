package me.feseis.fish.logistics.vo.staff;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/**
 * 签发培训凭证入参。岗位与网点均由签发方显式指定，无默认值。
 */
public record StaffTrainingIssueReqVO(

        @NotBlank(message = "手机号不能为空")
        @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
        String phone,

        @NotNull(message = "岗位不能为空")
        @Min(value = 1, message = "岗位不合法")
        @Max(value = 3, message = "岗位不合法")
        Integer staffRole,

        @NotBlank(message = "网点编码不能为空")
        String siteCode
) {
}
