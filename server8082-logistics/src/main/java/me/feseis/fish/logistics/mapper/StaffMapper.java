package me.feseis.fish.logistics.mapper;

import java.util.Optional;

import me.feseis.fish.logistics.mapper.model.StaffRow;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface StaffMapper {

    /** 一个账号最多一条人员记录（uk_auth_account），只取在职的。 */
    @Select("SELECT logistics_staff_id, auth_account_id, staff_no, staff_name, staff_phone, staff_role, "
            + "logistics_site_id, enabled, hired_at, left_at "
            + "FROM logistics_staff WHERE auth_account_id = #{accountId} AND enabled = 1")
    Optional<StaffRow> findOnDutyByAccountId(@Param("accountId") String accountId);

    /** 任意状态，用于入职前判断账号是否已有员工记录。 */
    @Select("SELECT logistics_staff_id, auth_account_id, staff_no, staff_name, staff_phone, staff_role, "
            + "logistics_site_id, enabled, hired_at, left_at "
            + "FROM logistics_staff WHERE auth_account_id = #{accountId}")
    Optional<StaffRow> findByAccountId(@Param("accountId") String accountId);

    @Insert("INSERT INTO logistics_staff (logistics_staff_id, auth_account_id, staff_no, staff_name, "
            + "staff_phone, staff_role, logistics_site_id, enabled, hired_at) VALUES ("
            + "#{logisticsStaffId}, #{authAccountId}, #{staffNo}, #{staffName}, "
            + "#{staffPhone}, #{staffRole}, #{logisticsSiteId}, #{enabled}, #{hiredAt})")
    void insert(StaffRow row);
}
