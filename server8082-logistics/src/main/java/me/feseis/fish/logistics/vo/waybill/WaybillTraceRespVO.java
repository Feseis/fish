package me.feseis.fish.logistics.vo.waybill;

import java.time.LocalDateTime;

/**
 * 轨迹条目。actionId 对应 trace_action 字典，名称由前端按字典渲染。
 */
public record WaybillTraceRespVO(
        Long traceId,
        Integer actionId,
        Integer statusAfter,
        Integer operatorType,
        String operatorName,
        LocalDateTime occurredAt
) {
}
