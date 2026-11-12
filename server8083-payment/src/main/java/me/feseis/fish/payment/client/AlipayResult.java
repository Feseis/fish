package me.feseis.fish.payment.client;

/**
 * 支付宝调用结果。rawBody 保留原始响应，便于排查验签与业务错误。
 */
public record AlipayResult(
        boolean success,
        String code,
        String subCode,
        String message,
        String qrCode,
        String tradeStatus,
        String tradeNo,
        String totalAmount,
        String rawBody
) {

    public static AlipayResult failure(String code, String subCode, String message, String rawBody) {
        return new AlipayResult(false, code, subCode, message, null, null, null, null, rawBody);
    }
}
