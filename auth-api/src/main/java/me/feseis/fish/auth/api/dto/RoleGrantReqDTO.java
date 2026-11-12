package me.feseis.fish.auth.api.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 内部授予角色入参。幂等：已授予视为成功。
 */
public record RoleGrantReqDTO(

        @NotBlank(message = "账号不能为空")
        @Size(max = 36, message = "账号过长")
        String accountId,

        @NotNull(message = "角色不能为空")
        @Min(value = 1, message = "角色不合法")
        @Max(value = 3, message = "角色不合法")
        Integer roleId
) {
}
