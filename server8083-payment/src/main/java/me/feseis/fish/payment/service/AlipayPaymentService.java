package me.feseis.fish.payment.service;

import java.time.Duration;
import java.time.LocalDateTime;

import lombok.RequiredArgsConstructor;
import me.feseis.fish.payment.client.AlipayClient;
import me.feseis.fish.payment.client.AlipayResult;
import me.feseis.fish.payment.exp.BizException;
import me.feseis.fish.payment.exp.ErrorCode;
import me.feseis.fish.payment.mapper.PaymentMapper;
import me.feseis.fish.payment.mapper.model.PaymentRow;
import org.springframework.stereotype.Service;

/**
 * 支付宝沙箱渠道适配：生成二维码、主动查单。入账逻辑在 PaymentPaidService。
 */
@Service
@RequiredArgsConstructor
public class AlipayPaymentService {

    private static final String TRADE_SUCCESS = "TRADE_SUCCESS";
    private static final String TRADE_FINISHED = "TRADE_FINISHED";
    private static final String SUB_CODE_NOT_EXIST = "TRADE_NOT_EXIST";

    private final AlipayClient alipayClient;
    private final PaymentMapper paymentMapper;
    private final PaymentPaidService paymentPaidService;

    public boolean isConfigured() {
        return alipayClient.isConfigured();
    }

    /** 生成二维码并落库，返回 qr_code 内容。 */
    public String precreate(PaymentRow row) {
        int minutes = (int) Math.max(1,
                Duration.between(LocalDateTime.now(), row.getExpireAt()).toMinutes());
        AlipayResult result = alipayClient.precreate(row.getPaymentNo(), row.getAmount(),
                "飞鱼速递 " + row.getPaymentNo(), minutes);
        if (!result.success() || result.qrCode() == null || result.qrCode().isBlank()) {
            throw BizException.of(ErrorCode.CHANNEL_ERROR, result.message());
        }
        paymentMapper.updateChannelPayUrl(row.getPaymentId(), result.qrCode());
        return result.qrCode();
    }

    /** 主动查单；已支付则入账。用户还没扫码时支付宝返回 TRADE_NOT_EXIST，属正常中间态。 */
    public void sync(PaymentRow row) {
        AlipayResult result = alipayClient.query(row.getPaymentNo());
        if (!result.success()) {
            if (result.subCode() != null && result.subCode().contains(SUB_CODE_NOT_EXIST)) {
                return;
            }
            throw BizException.of(ErrorCode.CHANNEL_ERROR, result.message());
        }
        if (isTradeSuccess(result.tradeStatus())) {
            paymentPaidService.applyPaid(row.getPaymentId(), result.tradeNo(), LocalDateTime.now());
        }
    }

    public static boolean isTradeSuccess(String tradeStatus) {
        return TRADE_SUCCESS.equals(tradeStatus) || TRADE_FINISHED.equals(tradeStatus);
    }
}
