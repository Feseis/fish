package me.feseis.fish.logistics.mapper.model;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class HandoverRow {
    private String handoverId;
    private String handoverNo;
    private String transportLineId;
    private Integer direction;
    private String fromSiteId;
    private String toSiteId;
    private String vehicleId;
    private String vehiclePlate;
    private String driverStaffId;
    private String driverName;
    private String operatorName;
    private LocalDateTime departedAt;
    private LocalDateTime arrivedAt;
    private Integer status;
    private LocalDateTime createdAt;
}
