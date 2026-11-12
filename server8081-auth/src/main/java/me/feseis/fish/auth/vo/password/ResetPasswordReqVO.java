package me.feseis.fish.auth.vo.password;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 通过短信验证码重置密码。
 */
public record ResetPasswordReqVO(

        @NotBlank(message = "手机号不能为空")
        @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
        String phone,

        @NotBlank(message = "验证码不能为空")
        @Pattern(regexp = "^\\d{6}$", message = "验证码必须是 6 位数字")
        String code,

        @NotBlank(message = "新密码不能为空")
        @Size(max = 64, message = "密码不得超过 64 位")
        String newPassword
) {
}
