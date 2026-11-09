package me.feseis.fish.logistics.exp;

import org.springframework.http.HttpStatus;

/**
 * 物流业务错误码。name() 即返回给前端的 code。
 */
public enum ErrorCode {

    INVALID_PARAM(HttpStatus.BAD_REQUEST, "参数校验失败"),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "登录已失效"),
    ADDRESS_NOT_FOUND(HttpStatus.NOT_FOUND, "地址不存在"),
    ADDRESS_DUPLICATED(HttpStatus.CONFLICT, "地址已存在"),
    PRICE_RULE_MISSING(HttpStatus.UNPROCESSABLE_ENTITY, "该寄收关系没有启用的运费规则"),
    INSURANCE_RULE_MISSING(HttpStatus.INTERNAL_SERVER_ERROR, "没有启用的保价规则"),
    INSURANCE_LIMIT_EXCEEDED(HttpStatus.UNPROCESSABLE_ENTITY, "申报价值超过保价上限"),
    AMAP_UNAVAILABLE(HttpStatus.BAD_GATEWAY, "地址服务不可用"),
    AUTH_UNAVAILABLE(HttpStatus.BAD_GATEWAY, "认证服务不可用"),
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
