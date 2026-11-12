package me.feseis.fish.logistics.mapper.model;

import java.time.LocalDateTime;

import lombok.Data;

@Data
public class EventOutboxRow {
    private Long eventOutboxId;
    private Integer targetService;
    private Integer eventType;
    private String authAccountId;
    private Integer authRoleId;
    private Integer bizType;
    private String bizId;
    private String waybillId;
    private Integer status;
    private Integer retryCount;
    private LocalDateTime nextRetryAt;
    private String lastError;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
