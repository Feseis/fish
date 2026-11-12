package me.feseis.fish.payment.service;

import java.time.LocalDateTime;
import java.util.Optional;

import lombok.RequiredArgsConstructor;
import me.feseis.fish.payment.exp.BizException;
import me.feseis.fish.payment.exp.ErrorCode;
import me.feseis.fish.payment.mapper.PaymentMapper;
import me.feseis.fish.payment.mapper.model.PaymentRow;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 渠道支付成功入账。sync 与异步通知共用，三种结果：正常入账、重复通知幂等、关闭后迟到支付。
 */
@Service
@RequiredArgsConstructor
public class PaymentPaidService {

    private static final int CHANNEL_ALIPAY = 2;
    private static final int STATUS_PAID = 2;
    private static final int STATUS_CLOSED = 4;

    private final PaymentMapper paymentMapper;

    @Transactional
    public void applyPaid(String paymentId, String tradeNo, LocalDateTime paidAt) {
        if (tradeNo == null || tradeNo.isBlank()) {
            throw BizException.of(ErrorCode.CHANNEL_ERROR, "渠道交易号为空");
        }

        Optional<PaymentRow> sameTrade = paymentMapper.findByChannelTradeNo(CHANNEL_ALIPAY, tradeNo);
        if (sameTrade.isPresent()) {
            return;
        }

        int updated = paymentMapper.applyPaid(paymentId, tradeNo, paidAt);
        if (updated == 1) {
            return;
        }

        PaymentRow current = paymentMapper.findById(paymentId)
                .orElseThrow(() -> BizException.of(ErrorCode.PAYMENT_NOT_FOUND));
        if (Integer.valueOf(STATUS_PAID).equals(current.getStatus())) {
            return;
        }
        if (Integer.valueOf(STATUS_CLOSED).equals(current.getStatus())) {
            paymentMapper.markLatePaid(paymentId, tradeNo, paidAt);
            return;
        }
        throw BizException.of(ErrorCode.PAYMENT_STATUS_INVALID);
    }
}
