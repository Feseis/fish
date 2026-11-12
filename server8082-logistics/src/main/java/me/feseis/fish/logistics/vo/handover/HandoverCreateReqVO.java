package me.feseis.fish.logistics.vo.handover;

import jakarta.validation.constraints.NotBlank;

/**
 * 创建交接单（班次）。from/to 由线路带出，调用方不填；创建后为待发状态。
 */
public record HandoverCreateReqVO(

        @NotBlank(message = "运输线路不能为空")
        String transportLineId,

        String vehicleId,

        String driverStaffId
) {
}
