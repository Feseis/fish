package me.feseis.fish.logistics.mapper.model;

import java.time.LocalDateTime;

import lombok.Data;

@Data
public class StaffTrainingRow {
    private String staffTrainingId;
    private Integer staffRole;
    private String logisticsSiteId;
    private String traineeAuthAccountId;
    private String traineeName;
    private String traineePhone;
    private LocalDateTime trainedAt;
    private Integer result;
    private String voucherCode;
    private LocalDateTime voucherExpireAt;
    private String issuedByStaffId;
    private String issuedByStaffName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
