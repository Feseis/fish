package me.feseis.fish.sms.api.dto;

import java.util.List;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 发送入参。requestId 由调用方生成，是幂等键；命中已有记录时直接返回当时的结果。
 */
public record SmsSendReqDTO(

        @NotBlank(message = "幂等号不能为空")
        @Size(max = 36, message = "幂等号过长")
        String requestId,

        @NotBlank(message = "模板码不能为空")
        @Size(max = 32, message = "模板码过长")
        String templateCode,

        @NotBlank(message = "接收手机号不能为空")
        @Size(max = 20, message = "手机号过长")
        String targetPhone,

        @NotNull(message = "调用方服务码不能为空")
        @Min(value = 1, message = "服务码不合法")
        @Max(value = 4, message = "服务码不合法")
        Integer sourceService,

        @Min(value = 1, message = "业务类型不合法")
        @Max(value = 5, message = "业务类型不合法")
        Integer bizType,

        @Size(max = 36, message = "业务主键过长")
        String bizId,

        @NotNull(message = "模板参数不能为空")
        List<@NotBlank(message = "模板参数不能为空") @Size(max = 200, message = "模板参数过长") String> params
) {
}
