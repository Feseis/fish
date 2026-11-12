package me.feseis.fish.logistics.mapper.model;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class StaffRow {
    private String logisticsStaffId;
    private String authAccountId;
    private String staffNo;
    private String staffName;
    private String staffPhone;
    private Integer staffRole;
    private String logisticsSiteId;
    private Integer enabled;
    private LocalDateTime hiredAt;
    private LocalDateTime leftAt;
}
