package me.feseis.fish.logistics.mapper.model;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class ApprovalRow {
    private String approvalId;
    private Integer bizType;
    private String bizId;
    private Integer status;
    private Integer applyOperatorType;
    private String applyOperatorId;
    private String applyOperatorName;
    private String applyRemark;
    private Integer auditOperatorType;
    private String auditOperatorId;
    private String auditOperatorName;
    private String auditRemark;
    private LocalDateTime auditedAt;
}
