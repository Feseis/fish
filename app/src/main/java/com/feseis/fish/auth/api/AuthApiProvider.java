package com.feseis.fish.auth.api;

/**
 * 认证接口的唯一出口。接入真实后端时只替换这里的实现。
 */
public final class AuthApiProvider {

    private static final AuthApi INSTANCE = new MockAuthApi();

    private AuthApiProvider() {
    }

    public static AuthApi get() {
        return INSTANCE;
    }
}
