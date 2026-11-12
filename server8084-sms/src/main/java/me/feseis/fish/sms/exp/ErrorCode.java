package me.feseis.fish.sms.exp;

import org.springframework.http.HttpStatus;

/**
 * 短信业务错误码。name() 即返回给调用方的 code。
 */
public enum ErrorCode {

    INVALID_PARAM(HttpStatus.BAD_REQUEST, "参数校验失败"),
    TEMPLATE_NOT_FOUND(HttpStatus.BAD_REQUEST, "模板不存在"),
    SMS_SEND_FAILED(HttpStatus.BAD_GATEWAY, "短信发送失败"),
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
