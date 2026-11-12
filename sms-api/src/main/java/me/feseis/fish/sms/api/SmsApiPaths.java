package me.feseis.fish.sms.api;

/**
 * 短信服务内部接口路径。服务间调用一律走 /internal 前缀。
 */
public final class SmsApiPaths {

    public static final String SEND = "/sms/internal/send";

    /** 查询手机号是否在短信沙盒登记，注册发码前校验。 */
    public static final String PHONE_EXISTS = "/sms/internal/phones/exists";

    private SmsApiPaths() {
    }
}
