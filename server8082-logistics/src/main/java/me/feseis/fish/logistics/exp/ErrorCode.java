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
    WAYBILL_NOT_FOUND(HttpStatus.NOT_FOUND, "运单不存在"),
    WAYBILL_STATUS_INVALID(HttpStatus.CONFLICT, "运单当前状态不允许该操作"),
    HANDOVER_NOT_FOUND(HttpStatus.NOT_FOUND, "交接单不存在"),
    HANDOVER_STATUS_INVALID(HttpStatus.CONFLICT, "交接单当前状态不允许该操作"),
    ROUTE_MISSING(HttpStatus.UNPROCESSABLE_ENTITY, "运单没有可用的下一跳线路"),
    CANCEL_DUPLICATED(HttpStatus.CONFLICT, "该运单已提交取消"),
    STAFF_FORBIDDEN(HttpStatus.FORBIDDEN, "需要物流员工身份"),
    AUDIT_UNAVAILABLE(HttpStatus.BAD_GATEWAY, "审核服务不可用"),
    PAYMENT_UNAVAILABLE(HttpStatus.BAD_GATEWAY, "支付服务不可用"),
    DELIVERY_AREA_UNCOVERED(HttpStatus.UNPROCESSABLE_ENTITY, "地址超出服务范围"),
    PRICE_RULE_MISSING(HttpStatus.UNPROCESSABLE_ENTITY, "该寄收关系没有启用的运费规则"),
    INSURANCE_RULE_MISSING(HttpStatus.INTERNAL_SERVER_ERROR, "没有启用的保价规则"),
    INSURANCE_LIMIT_EXCEEDED(HttpStatus.UNPROCESSABLE_ENTITY, "申报价值超过保价上限"),
    AMAP_UNAVAILABLE(HttpStatus.BAD_GATEWAY, "地址服务不可用"),
    AUTH_UNAVAILABLE(HttpStatus.BAD_GATEWAY, "认证服务不可用"),
    ACCOUNT_NOT_FOUND(HttpStatus.NOT_FOUND, "账号不存在"),
    REAL_NAME_REQUIRED(HttpStatus.UNPROCESSABLE_ENTITY, "账号姓名未完善"),
    SITE_NOT_FOUND(HttpStatus.NOT_FOUND, "网点不存在"),
    VOUCHER_EXISTS(HttpStatus.CONFLICT, "该账号该岗位已有培训凭证"),
    VOUCHER_NOT_FOUND(HttpStatus.NOT_FOUND, "培训凭证不存在"),
    VOUCHER_EXPIRED(HttpStatus.UNPROCESSABLE_ENTITY, "培训凭证已失效"),
    VOUCHER_ACCOUNT_MISMATCH(HttpStatus.FORBIDDEN, "培训凭证不属于当前账号"),
    ALREADY_STAFF(HttpStatus.CONFLICT, "该账号已是物流员工"),
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
