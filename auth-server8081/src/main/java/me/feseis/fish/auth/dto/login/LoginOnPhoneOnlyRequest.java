package me.feseis.fish.auth.dto.login;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * 验证码登录入参。phone 为唯一登录标识，code 为 6 位数字验证码。
 */
public record LoginOnPhoneOnlyRequest(
        @NotBlank(message = "手机号不能为空")
        @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
        String phone,

        @NotBlank(message = "验证码不能为空")
        @Pattern(regexp = "^\\d{6}$", message = "验证码格式不正确")
        String code
) {
    @Override
    public String toString() {
        return "LoginOnPhoneOnlyRequest[phone=" + phone + ", code=***]";
    }
}
