package me.feseis.fish.logistics.vo.staff;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 入职申请入参。岗位与归属网点由凭证决定，申请人只提供凭证码与备注。
 */
public record StaffApplyReqVO(

        @NotBlank(message = "凭证码不能为空")
        @Pattern(regexp = "^[A-Za-z0-9]{12}$", message = "凭证码为 12 位字母数字")
        String voucherCode,

        @Size(max = 255, message = "备注不得超过 255 字")
        String applyRemark
) {
}
