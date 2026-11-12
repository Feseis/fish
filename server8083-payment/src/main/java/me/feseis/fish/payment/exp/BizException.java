package me.feseis.fish.payment.exp;

import lombok.Getter;

/**
 * 业务异常。只承载「错误码 + 可选的补充描述」，HTTP 状态由 ErrorCode 决定。
 */
@Getter
public class BizException extends RuntimeException {

    private final ErrorCode errorCode;
    private final String detail;

    private BizException(ErrorCode errorCode, String detail) {
        super(detail == null || detail.isBlank() ? errorCode.message() : detail);
        this.errorCode = errorCode;
        this.detail = detail;
    }

    public static BizException of(ErrorCode errorCode) {
        return new BizException(errorCode, null);
    }

    public static BizException of(ErrorCode errorCode, String detail) {
        return new BizException(errorCode, detail);
    }
}
