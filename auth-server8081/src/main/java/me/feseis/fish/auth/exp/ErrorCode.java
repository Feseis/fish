package me.feseis.fish.auth.exp;

import org.springframework.http.HttpStatus;

/**
 * 业务错误码。name() 即返回给前端的 code，需与 auth_login_log.fail_reason 保持同一套词表。
 */
public enum ErrorCode {

    PWD_WRONG(HttpStatus.UNAUTHORIZED, "手机号或密码错误"),
    NO_ACCOUNT(HttpStatus.UNAUTHORIZED, "账号不存在"),
    FROZEN(HttpStatus.FORBIDDEN, "账号已冻结"),
    TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "登录已失效"),
    INVALID_PARAM(HttpStatus.BAD_REQUEST, "参数校验失败"),
    RATE_LIMITED(HttpStatus.TOO_MANY_REQUESTS, "请求过于频繁"),
    NOT_FOUND(HttpStatus.NOT_FOUND, "资源不存在"),
    INTERNAL(HttpStatus.INTERNAL_SERVER_ERROR, "系统内部错误");

    private final HttpStatus status;
    private final String message;

    ErrorCode(HttpStatus status, String message) {
        this.status = status;
        this.message = message;
    }

    public String code() {
        return name();
    }

    public HttpStatus status() {
        return status;
    }

    public String message() {
        return message;
    }
}
