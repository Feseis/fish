package me.feseis.fish.payment.mapper.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import lombok.Data;

@Data
public class PaymentRow {
    private String paymentId;
    private String paymentNo;
    private Integer bizType;
    private String bizId;

    private Integer payerType;
    private String payerAccountId;
    private String payerPhone;

    private BigDecimal amount;
    private Integer payChannel;
    private Integer status;
    private String channelTradeNo;
    private String paidBizKey;
    private String payRemark;
    private String channelPayUrl;

    private String payToken;
    private Integer remindCount;
    private LocalDateTime nextRemindAt;

    private LocalDateTime expireAt;
    private LocalDateTime paidAt;
    private LocalDateTime closedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
