package me.feseis.fish.auth.api.dto;

/**
 * 按手机号查账号的内部出参。realName 是 auth 的权威姓名，业务侧只做快照。
 */
public record AccountLookupRespDTO(
        String accountId,
        String phone,
        Integer status,
        String realName
) {
}
