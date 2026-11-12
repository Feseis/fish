package me.feseis.fish.logistics.mapper.model;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class WaybillCancelRow {
    private String waybillCancelId;
    private String waybillId;
    private Integer applyRound;
    private Integer cancelReasonId;
    private String cancelRemark;
    private Integer operatorType;
    private String operatorId;
    private String operatorName;
    private Integer statusBefore;
    private LocalDateTime cancelledAt;
    private LocalDateTime createdAt;
}
