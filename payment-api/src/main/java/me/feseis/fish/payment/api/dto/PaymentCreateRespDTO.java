package me.feseis.fish.payment.api.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 建单出参。服务间接口只暴露支付单必要字段，不随对外 VO 演进。
 */
public record PaymentCreateRespDTO(
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
