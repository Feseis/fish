package me.feseis.fish.auth.dto.login;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 登录入参。phone 为唯一登录标识，password 上限 64 字符以规避 BCrypt 72 字节截断。
 */
public record LoginOnPasswordRequest(
        @NotBlank(message = "手机号不能为空")
        @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
        String phone,

        @NotBlank(message = "密码不能为空")
        @Size(max = 64, message = "密码长度不得超过 64")
        String password
) {
    @Override
    public String toString() {
        return "LoginOnPasswordRequest[phone=" + phone + ", password=***]";
    }
}
