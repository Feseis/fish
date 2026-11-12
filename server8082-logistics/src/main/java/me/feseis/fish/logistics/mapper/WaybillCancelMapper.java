package me.feseis.fish.logistics.mapper;

import me.feseis.fish.logistics.mapper.model.WaybillCancelRow;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface WaybillCancelMapper {

    @Select("SELECT COUNT(*) FROM waybill_cancel WHERE waybill_id = #{waybillId}")
    int countByWaybillId(@Param("waybillId") String waybillId);

    @Insert("INSERT INTO waybill_cancel (waybill_cancel_id, waybill_id, apply_round, cancel_reason_id, "
            + "cancel_remark, operator_type, operator_id, operator_name, status_before, cancelled_at) VALUES ("
            + "#{waybillCancelId}, #{waybillId}, #{applyRound}, #{cancelReasonId}, "
            + "#{cancelRemark}, #{operatorType}, #{operatorId}, #{operatorName}, #{statusBefore}, #{cancelledAt})")
    void insert(WaybillCancelRow row);
}
