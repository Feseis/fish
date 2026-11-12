package me.feseis.fish.logistics.mapper.model;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class WaybillExceptionRow {
    private String waybillExceptionId;
    private String waybillId;
    private Integer exceptionReasonId;
    private String exceptionRemark;
    private Integer operatorType;
    private String operatorId;
    private String operatorName;
    private Integer statusBefore;
    private LocalDateTime createdAt;
}
