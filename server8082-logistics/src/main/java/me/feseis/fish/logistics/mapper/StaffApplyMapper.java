package me.feseis.fish.logistics.mapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;

public interface StaffApplyMapper {

    @Insert("INSERT INTO staff_apply (staff_apply_id, auth_account_id, staff_training_id, "
            + "apply_staff_role, apply_remark) VALUES ("
            + "#{staffApplyId}, #{accountId}, #{staffTrainingId}, #{applyStaffRole}, #{applyRemark})")
    void insert(@Param("staffApplyId") String staffApplyId,
                @Param("accountId") String accountId,
                @Param("staffTrainingId") String staffTrainingId,
                @Param("applyStaffRole") Integer applyStaffRole,
                @Param("applyRemark") String applyRemark);
}
