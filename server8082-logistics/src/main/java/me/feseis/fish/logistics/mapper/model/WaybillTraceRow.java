package me.feseis.fish.logistics.mapper.model;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class WaybillTraceRow {
    private Long waybillTraceId;
    private String waybillId;
    private Integer operatorType;
    private String operatorId;
    private String operatorName;
    private Integer traceActionId;
    private Integer statusAfter;
    private LocalDateTime occurredAt;
    private LocalDateTime createdAt;
}
