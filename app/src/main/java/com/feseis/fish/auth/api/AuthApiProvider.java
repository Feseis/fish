package com.feseis.fish.auth.api;

import android.content.Context;

/**
 * 认证接口的唯一出口。启动时由 FishApp 初始化，更换实现只改这里。
 */
public final class AuthApiProvider {

    private static AuthApi instance;

    private AuthApiProvider() {
    }

    public static void init(Context context) {
        if (instance == null) {
            instance = new HttpAuthApi(context);
        }
    }

    public static AuthApi get() {
        if (instance == null) {
            throw new IllegalStateException("AuthApiProvider 未初始化，请检查 FishApp");
        }
        return instance;
    }
}
