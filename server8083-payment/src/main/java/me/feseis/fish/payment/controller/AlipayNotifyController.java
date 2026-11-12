package me.feseis.fish.payment.controller;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.feseis.fish.payment.client.AlipayClient;
import me.feseis.fish.payment.mapper.PaymentMapper;
import me.feseis.fish.payment.mapper.model.PaymentRow;
import me.feseis.fish.payment.service.AlipayPaymentService;
import me.feseis.fish.payment.service.PaymentPaidService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 支付宝异步通知。必须返回纯文本 success，返回 JSON 会被支付宝当成失败并重试 24 小时。
 * 公开接口，安全性完全依赖验签，不要求登录态。
 */
@Slf4j
@RestController
@RequiredArgsConstructor
public class AlipayNotifyController {

    private static final DateTimeFormatter GMT_PAYMENT_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final AlipayClient alipayClient;
    private final PaymentMapper paymentMapper;
    private final PaymentPaidService paymentPaidService;

    @PostMapping("/payment/alipay/notify")
    String notify(@RequestParam Map<String, String> params) {
        if (!alipayClient.verifyNotify(params)) {
            log.warn("支付宝通知验签失败 outTradeNo={}", params.get("out_trade_no"));
            return "failure";
        }
        if (!AlipayPaymentService.isTradeSuccess(params.get("trade_status"))) {
            return "success";
        }

        String outTradeNo = params.get("out_trade_no");
        PaymentRow row = paymentMapper.findByPaymentNo(outTradeNo).orElse(null);
        if (row == null) {
            log.warn("支付宝通知找不到支付单 outTradeNo={}", outTradeNo);
            return "failure";
        }

        if (!amountMatches(params.get("total_amount"), row.getAmount())) {
            log.warn("支付宝通知金额不一致 outTradeNo={} notify={} local={}",
                    outTradeNo, params.get("total_amount"), row.getAmount());
            return "failure";
        }

        try {
            paymentPaidService.applyPaid(row.getPaymentId(), params.get("trade_no"), paidAt(params));
            return "success";
        } catch (RuntimeException e) {
            log.error("支付宝通知入账失败 outTradeNo={}", outTradeNo, e);
            return "failure";
        }
    }

    private static boolean amountMatches(String notifyAmount, BigDecimal localAmount) {
        if (notifyAmount == null || notifyAmount.isBlank()) {
            return false;
        }
        try {
            return new BigDecimal(notifyAmount).compareTo(localAmount) == 0;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private static LocalDateTime paidAt(Map<String, String> params) {
        String gmtPayment = params.get("gmt_payment");
        if (gmtPayment != null && !gmtPayment.isBlank()) {
            try {
                return LocalDateTime.parse(gmtPayment, GMT_PAYMENT_FORMAT);
            } catch (RuntimeException ignored) {
                // 时间格式异常时退回当前时间，不影响入账正确性
            }
        }
        return LocalDateTime.now();
    }
}
