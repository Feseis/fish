package me.feseis.fish.logistics.vo.handover;

import java.time.LocalDateTime;
import java.util.List;

public record HandoverRespVO(
        String handoverId,
        String handoverNo,
        String transportLineId,
        Integer direction,
        String fromSiteId,
        String toSiteId,
        String vehiclePlate,
        String driverName,
        LocalDateTime departedAt,
        LocalDateTime arrivedAt,
        Integer status,
        List<String> waybillIds
) {
}
