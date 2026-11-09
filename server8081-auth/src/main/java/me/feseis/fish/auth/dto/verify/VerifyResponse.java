package me.feseis.fish.auth.dto.verify;

import java.util.List;

/**
 * 令牌校验出参：下游凭此获得调用者身份与角色，不再回传任何令牌。
 */
public record VerifyResponse(
        String sessionId,
        String accountId,
        List<Integer> roles
) {
}
