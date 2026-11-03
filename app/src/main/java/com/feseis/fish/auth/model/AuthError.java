package com.feseis.fish.auth.model;

/**
 * 客户端统一错误。code 与后端 ErrorCode 的名称保持一致。
 */
public final class AuthError {

    public static final String PWD_WRONG = "PWD_WRONG";
    public static final String SMS_WRONG = "SMS_WRONG";
    public static final String SMS_EXPIRED = "SMS_EXPIRED";
    public static final String NO_ACCOUNT = "NO_ACCOUNT";
    public static final String FROZEN = "FROZEN";
    public static final String CLOSED = "CLOSED";
    public static final String PHONE_EXISTS = "PHONE_EXISTS";
    public static final String TOKEN_EXPIRED = "TOKEN_EXPIRED";
    public static final String INVALID_PARAM = "INVALID_PARAM";
    public static final String RATE_LIMITED = "RATE_LIMITED";
    public static final String SMS_SEND_FAILED = "SMS_SEND_FAILED";
    public static final String NOT_FOUND = "NOT_FOUND";
    public static final String INTERNAL = "INTERNAL";
    public static final String NETWORK = "NETWORK";

    private final String code;
    private final String message;

    public AuthError(String code, String message) {
        this.code = code;
        this.message = message;
    }

    public String getCode() {
        return this.code;
    }

    public String getMessage() {
        return this.message;
    }
}
