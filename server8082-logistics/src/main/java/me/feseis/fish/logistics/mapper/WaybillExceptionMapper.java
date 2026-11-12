package me.feseis.fish.logistics.mapper;

import java.util.Optional;
import me.feseis.fish.logistics.mapper.model.WaybillExceptionRow;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface WaybillExceptionMapper {

    @Insert("INSERT INTO waybill_exception (waybill_exception_id, waybill_id, exception_reason_id, "
            + "exception_remark, operator_type, operator_id, operator_name, status_before) VALUES ("
            + "#{waybillExceptionId}, #{waybillId}, #{exceptionReasonId}, "
            + "#{exceptionRemark}, #{operatorType}, #{operatorId}, #{operatorName}, #{statusBefore})")
    void insert(WaybillExceptionRow row);

    /** 取最近一条异常，用于判断异常来源状态。 */
    @Select("SELECT waybill_exception_id, waybill_id, exception_reason_id, exception_remark, "
            + "operator_type, operator_id, operator_name, status_before, created_at "
            + "FROM waybill_exception WHERE waybill_id = #{waybillId} "
            + "ORDER BY created_at DESC, waybill_exception_id DESC LIMIT 1")
    Optional<WaybillExceptionRow> findLatestByWaybillId(@Param("waybillId") String waybillId);
}
