package me.feseis.fish.auth.exp;

/**
 * 业务异常。只承载“错误码 + 可选的补充描述”，HTTP 状态由 ErrorCode 决定。
 */
public class AuthException extends RuntimeException {

    private final ErrorCode errorCode;
    private final String detail;

    private AuthException(ErrorCode errorCode, String detail) {
        super(detail == null || detail.isBlank() ? errorCode.message() : detail);
        this.errorCode = errorCode;
        this.detail = detail;
    }

    public static AuthException of(ErrorCode errorCode) {
        return new AuthException(errorCode, null);
    }

    public static AuthException of(ErrorCode errorCode, String detail) {
        return new AuthException(errorCode, detail);
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }

    public String getDetail() {
        return detail;
    }
}
