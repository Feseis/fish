package me.feseis.fish.contract;

/**
 * 跨端 HTTP 协议常量：App 与各后端服务共用，避免字面量散落。
 * 只放协议定义，不放 Spring / Android 代码。
 */
public final class AuthHeaders {

    public static final String AUTHORIZATION = "Authorization";
    public static final String BEARER_PREFIX = "Bearer ";

    private AuthHeaders() {
    }
}
