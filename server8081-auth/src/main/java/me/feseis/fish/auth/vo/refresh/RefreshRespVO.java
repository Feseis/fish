package me.feseis.fish.auth.vo.refresh;

import java.time.OffsetDateTime;

/**
 * 续期出参。refresh 令牌不轮换，因此响应体只回新的短期令牌。
 */
public record RefreshRespVO(
        String accessToken,
        OffsetDateTime accessExpiresAt
) {
    @Override
    public String toString() {
        return "RefreshRespVO[accessToken=***, accessExpiresAt=" + accessExpiresAt + "]";
    }
}
