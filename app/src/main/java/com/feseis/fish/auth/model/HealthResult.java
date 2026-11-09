package com.feseis.fish.auth.model;

/**
 * 对应后端 /auth/health 的 HealthStatus：任一项为 false 时后端返回 503。
 */
public final class HealthResult {

    private final boolean database;
    private final boolean redis;

    public HealthResult(boolean database, boolean redis) {
        this.database = database;
        this.redis = redis;
    }

    public boolean isDatabase() {
        return this.database;
    }

    public boolean isRedis() {
        return this.redis;
    }
}
