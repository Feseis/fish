package me.feseis.fish.sms.api.dto;

/**
 * 手机号沙盒登记查询结果。registered=false 仍返回 200，由调用方决定如何处理。
 */
public record PhoneExistsRespDTO(
        String phone,
        Boolean registered
) {
}
