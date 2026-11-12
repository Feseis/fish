package me.feseis.fish.payment.api;

/**
 * 支付服务内部接口路径。服务间调用一律走 /internal 前缀。
 */
public final class PaymentApiPaths {

    public static final String CREATE_INTERNAL = "/payment/internal/orders";

    /** 业务终止（运单取消或退回）：关闭待支付单，已支付单建全额退款。 */
    public static final String BIZ_TERMINATED = "/payment/internal/biz-terminated";

    private PaymentApiPaths() {
    }
}
