package me.feseis.fish.auth.vo.register;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 注册入参。confirmPassword 不参与持久化，仅用于在 DTO 层校验两次输入一致。
 */
public record RegisterReqVO(
        @NotBlank(message = "手机号不能为空")
        @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
        String phone,

        @NotBlank(message = "验证码不能为空")
        @Pattern(regexp = "^\\d{6}$", message = "验证码格式不正确")
        String code,

        @NotBlank(message = "密码不能为空")
        @Size(max = 64, message = "密码长度不得超过 64")
        String password,

        @NotBlank(message = "确认密码不能为空")
        String confirmPassword
) {

    @AssertTrue(message = "两次输入的密码不一致")
    public boolean isPasswordConfirmed() {
        return password != null && password.equals(confirmPassword);
    }

    @Override
    public String toString() {
        return "RegisterReqVO[phone=" + phone + ", code=***, password=***]";
    }
}
