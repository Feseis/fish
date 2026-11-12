package me.feseis.fish.payment.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 支付单出参。payToken 仅在匿名到付时非空；qrCode 仅在沙箱渠道未支付时有值。
 */
public record PaymentRespVO(
        String paymentId,
        String paymentNo,
        Integer bizType,
        String bizId,
        BigDecimal amount,
        Integer payChannel,
        Integer status,
        String payToken,
        String qrCode,
        String payRemark,
        LocalDateTime expireAt,
        LocalDateTime paidAt,
        LocalDateTime closedAt,
        LocalDateTime createdAt
) {
}
