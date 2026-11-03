package com.feseis.fish.auth.model;

/**
 * 对应后端 RefreshResponse；refresh 令牌不轮换，响应只回新的短期令牌。
 */
public final class RefreshResult {

    private final String accessToken;
    private final String accessExpiresAt;

    public RefreshResult(String accessToken, String accessExpiresAt) {
        this.accessToken = accessToken;
        this.accessExpiresAt = accessExpiresAt;
    }

    public String getAccessToken() {
        return this.accessToken;
    }

    public String getAccessExpiresAt() {
        return this.accessExpiresAt;
    }
}
