package me.feseis.fish.auth.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 内部建账号入参。仅用于演示数据初始化，不校验验证码与密码复杂度。
 *
 * @param realName 可为空；为空时资料行只落账号关联，不写姓名
 */
public record AccountCreateReqDTO(

        @NotBlank(message = "手机号不能为空")
        @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
        String phone,

        @NotBlank(message = "密码不能为空")
        @Size(max = 64, message = "密码长度不得超过 64")
        String password,

        @Size(max = 32, message = "姓名过长")
        String realName
) {
}
