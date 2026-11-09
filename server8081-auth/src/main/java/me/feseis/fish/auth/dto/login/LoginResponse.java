package me.feseis.fish.auth.dto.login;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * 登录出参，密码登录与验证码登录共用。refresh 令牌通过 Set-Cookie 下发，不出现在响应体；phone 为脱敏值。
 */
public record LoginResponse(
        String accountId,
        String phone,
        List<Integer> roles,
        String accessToken,
        OffsetDateTime accessExpiresAt
) {
    @Override
    public String toString() {
        return "LoginResponse[accountId=" + accountId + ", phone=" + phone
                + ", roles=" + roles + ", accessToken=***, accessExpiresAt=" + accessExpiresAt + "]";
    }
}
