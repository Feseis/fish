package me.feseis.fish.auth.api.dto;

import java.util.List;

/**
 * 内部令牌校验出参。只服务其他服务，不含任何令牌原值。
 */
public record VerifyRespDTO(
        String sessionId,
        String accountId,
        List<Integer> roles
) {
}
