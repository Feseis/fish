package me.feseis.fish.payment.exp;

import org.springframework.http.HttpStatus;

/**
 * 支付业务错误码。name() 即返回给前端的 code。
 */
public enum ErrorCode {

    INVALID_PARAM(HttpStatus.BAD_REQUEST, "参数校验失败"),
    PAYMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "支付单不存在"),
    PAYMENT_STATUS_INVALID(HttpStatus.CONFLICT, "支付单当前状态不允许该操作"),
    PAYMENT_AMOUNT_INVALID(HttpStatus.UNPROCESSABLE_ENTITY, "支付金额必须大于 0"),
    CHANNEL_NOT_SUPPORTED(HttpStatus.UNPROCESSABLE_ENTITY, "支付渠道未接入"),
    CHANNEL_ERROR(HttpStatus.BAD_GATEWAY, "支付渠道调用失败"),
    DUPLICATE_PAYMENT(HttpStatus.CONFLICT, "该业务已有成功支付"),
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
