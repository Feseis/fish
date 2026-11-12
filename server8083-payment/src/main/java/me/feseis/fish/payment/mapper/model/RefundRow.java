package me.feseis.fish.payment.mapper.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import lombok.Data;

@Data
public class RefundRow {
    private String refundId;
    private String refundNo;
    private String paymentId;
    private BigDecimal amount;
    private Integer refundSource;
    private String reason;
    private Integer status;
    private String channelRefundNo;
    private LocalDateTime refundedAt;
    private String refundRemark;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
