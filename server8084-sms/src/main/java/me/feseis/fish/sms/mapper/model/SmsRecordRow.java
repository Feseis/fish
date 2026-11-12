package me.feseis.fish.sms.mapper.model;

import java.time.LocalDateTime;

import lombok.Data;

@Data
public class SmsRecordRow {
    private Long smsRecordId;
    private String requestId;
    private String templateCode;
    private String targetPhone;
    private String content;
    private Integer sourceService;
    private Integer bizType;
    private String bizId;
    private Integer result;
    private String failReason;
    private LocalDateTime createdAt;
}
