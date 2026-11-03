package com.feseis.fish.auth.store;

import com.feseis.fish.auth.model.LoginResult;

/**
 * 内存登录态。access 令牌只放内存；refresh 令牌在真实阶段由 CookieJar 托管。
 */
public final class SessionStore {

    private static final SessionStore INSTANCE = new SessionStore();

    private LoginResult session;

    private SessionStore() {
    }

    public static SessionStore get() {
        return INSTANCE;
    }

    public synchronized void save(LoginResult result) {
        this.session = result;
    }

    public synchronized LoginResult getSession() {
        return this.session;
    }

    public synchronized boolean isLoggedIn() {
        return this.session != null;
    }

    public synchronized void clear() {
        this.session = null;
    }
}
