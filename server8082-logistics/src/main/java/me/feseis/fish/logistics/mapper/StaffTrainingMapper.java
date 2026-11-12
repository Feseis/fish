package me.feseis.fish.logistics.mapper;

import java.time.LocalDateTime;
import java.util.Optional;

import me.feseis.fish.logistics.mapper.model.StaffTrainingRow;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

public interface StaffTrainingMapper {

    @Insert("INSERT INTO staff_training (staff_training_id, staff_role, logistics_site_id, "
            + "trainee_auth_account_id, trainee_name, trainee_phone, trained_at, result, "
            + "voucher_code, voucher_expire_at, issued_by_staff_id, issued_by_staff_name) VALUES ("
            + "#{staffTrainingId}, #{staffRole}, #{logisticsSiteId}, #{traineeAuthAccountId}, "
            + "#{traineeName}, #{traineePhone}, #{trainedAt}, #{result}, "
            + "#{voucherCode}, #{voucherExpireAt}, #{issuedByStaffId}, #{issuedByStaffName})")
    void insert(StaffTrainingRow row);

    /** 同一账号同一岗位唯一，用于发证前查重。 */
    @Select("SELECT * FROM staff_training WHERE trainee_auth_account_id = #{accountId} "
            + "AND staff_role = #{staffRole}")
    Optional<StaffTrainingRow> findByAccountAndRole(@Param("accountId") String accountId,
                                                    @Param("staffRole") Integer staffRole);

    /** 入职时按凭证码加行锁，避免同一凭证并发使用。 */
    @Select("SELECT * FROM staff_training WHERE voucher_code = #{voucherCode} FOR UPDATE")
    Optional<StaffTrainingRow> findByVoucherForUpdate(@Param("voucherCode") String voucherCode);

    /** 使用即失效：把有效期置为当前时刻。 */
    @Update("UPDATE staff_training SET voucher_expire_at = #{expireAt} "
            + "WHERE staff_training_id = #{staffTrainingId}")
    int expireVoucher(@Param("staffTrainingId") String staffTrainingId,
                      @Param("expireAt") LocalDateTime expireAt);
}
