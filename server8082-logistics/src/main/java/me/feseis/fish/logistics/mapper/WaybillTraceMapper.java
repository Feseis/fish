package me.feseis.fish.logistics.mapper;

import java.util.List;
import me.feseis.fish.logistics.mapper.model.WaybillTraceRow;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface WaybillTraceMapper {

    @Insert("INSERT INTO waybill_trace (waybill_id, operator_type, operator_id, operator_name, "
            + "trace_action_id, status_after, occurred_at) VALUES ("
            + "#{waybillId}, #{operatorType}, #{operatorId}, #{operatorName}, "
            + "#{traceActionId}, #{statusAfter}, #{occurredAt})")
    void insert(WaybillTraceRow row);

    @Select("SELECT waybill_trace_id, waybill_id, operator_type, operator_id, operator_name, "
            + "trace_action_id, status_after, occurred_at, created_at "
            + "FROM waybill_trace WHERE waybill_id = #{waybillId} "
            + "ORDER BY occurred_at ASC, waybill_trace_id ASC")
    List<WaybillTraceRow> listByWaybillId(@Param("waybillId") String waybillId);
}
