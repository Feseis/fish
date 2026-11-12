package me.feseis.fish.logistics.mapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;

public interface WaybillReturnMapper {

    @Insert("INSERT INTO waybill_return (waybill_return_id, waybill_id, return_reason_id, return_remark, "
            + "operator_type, operator_id, operator_name, status_before, completed_at) VALUES ("
            + "#{waybillReturnId}, #{waybillId}, #{returnReasonId}, #{returnRemark}, "
            + "#{operatorType}, #{operatorId}, #{operatorName}, #{statusBefore}, NULL)")
    void insert(@Param("waybillReturnId") String waybillReturnId,
                @Param("waybillId") String waybillId,
                @Param("returnReasonId") Integer returnReasonId,
                @Param("returnRemark") String returnRemark,
                @Param("operatorType") Integer operatorType,
                @Param("operatorId") String operatorId,
                @Param("operatorName") String operatorName,
                @Param("statusBefore") Integer statusBefore);
}
