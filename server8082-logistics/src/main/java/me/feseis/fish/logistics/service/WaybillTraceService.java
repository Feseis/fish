package me.feseis.fish.logistics.service;

import java.time.LocalDateTime;
import java.util.List;

import lombok.RequiredArgsConstructor;
import me.feseis.fish.logistics.mapper.WaybillTraceMapper;
import me.feseis.fish.logistics.mapper.model.WaybillTraceRow;
import org.springframework.stereotype.Service;

/**
 * 轨迹写入与查询。append-only，不更新、不删除。
 */
@Service
@RequiredArgsConstructor
public class WaybillTraceService {

    private final WaybillTraceMapper waybillTraceMapper;

    public void write(String waybillId,
                      int operatorType,
                      String operatorId,
                      String operatorName,
                      int traceActionId,
                      int statusAfter) {
        WaybillTraceRow row = new WaybillTraceRow();
        row.setWaybillId(waybillId);
        row.setOperatorType(operatorType);
        row.setOperatorId(operatorId);
        row.setOperatorName(operatorName);
        row.setTraceActionId(traceActionId);
        row.setStatusAfter(statusAfter);
        row.setOccurredAt(LocalDateTime.now());
        waybillTraceMapper.insert(row);
    }

    public List<WaybillTraceRow> list(String waybillId) {
        return waybillTraceMapper.listByWaybillId(waybillId);
    }
}
