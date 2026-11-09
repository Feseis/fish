package me.feseis.fish.auth.dto.refresh;

import java.time.OffsetDateTime;

/**
 * 续期出参。refresh 令牌不轮换，因此响应体只回新的短期令牌。
 */
public record RefreshResponse(
        String accessToken,
        OffsetDateTime accessExpiresAt
) {
    @Override
    public String toString() {
        return "RefreshResponse[accessToken=***, accessExpiresAt=" + accessExpiresAt + "]";
    }
}
