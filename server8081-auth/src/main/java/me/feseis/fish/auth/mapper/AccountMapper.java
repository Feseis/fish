package me.feseis.fish.auth.mapper;

import java.util.List;
import java.util.Optional;

import me.feseis.fish.auth.mapper.model.AccountDetailRow;
import me.feseis.fish.auth.mapper.model.AccountRow;
import me.feseis.fish.auth.mapper.model.PasswordRow;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface AccountMapper {

    @Select("SELECT auth_account_id AS account_id, phone, status FROM auth_account WHERE phone = #{phone}")
    Optional<AccountRow> findByPhone(@Param("phone") String phone);

    @Select("SELECT status FROM auth_account WHERE auth_account_id = #{accountId}")
    Optional<Integer> findStatus(@Param("accountId") String accountId);

    /** 按手机号取账号与权威姓名。资料行缺失时 real_name 为 NULL。 */
    @Select("SELECT a.auth_account_id AS account_id, a.phone, a.status, p.real_name "
            + "FROM auth_account a "
            + "LEFT JOIN auth_user_profile p ON p.auth_account_id = a.auth_account_id "
            + "WHERE a.phone = #{phone}")
    Optional<AccountDetailRow> findDetailByPhone(@Param("phone") String phone);

    @Select("SELECT password_hash, pwd_version FROM auth_password WHERE auth_account_id = #{accountId}")
    Optional<PasswordRow> findPassword(@Param("accountId") String accountId);

    @Select("SELECT auth_role_id AS role_id FROM auth_account_role WHERE auth_account_id = #{accountId} ORDER BY auth_role_id")
    List<Integer> findRoleIds(@Param("accountId") String accountId);

    @Insert("INSERT INTO auth_account (auth_account_id, phone, status) VALUES (#{accountId}, #{phone}, 1)")
    void insertAccount(@Param("accountId") String accountId, @Param("phone") String phone);

    @Insert("INSERT INTO auth_user_profile (auth_user_profile_id, auth_account_id) "
            + "VALUES (#{profileId}, #{accountId})")
    void insertProfile(@Param("profileId") String profileId, @Param("accountId") String accountId);

    /** 内部初始化时带姓名；realName 可为空。 */
    @Insert("INSERT INTO auth_user_profile (auth_user_profile_id, auth_account_id, real_name) "
            + "VALUES (#{profileId}, #{accountId}, #{realName})")
    void insertProfileWithName(@Param("profileId") String profileId,
                               @Param("accountId") String accountId,
                               @Param("realName") String realName);

    @Insert("INSERT INTO auth_password (auth_password_id, auth_account_id, password_hash, hash_algo, pwd_version) "
            + "VALUES (#{passwordId}, #{accountId}, #{passwordHash}, 'bcrypt', 0)")
    void insertPassword(@Param("passwordId") String passwordId,
                        @Param("accountId") String accountId,
                        @Param("passwordHash") String passwordHash);

    @Insert("INSERT INTO auth_account_role (auth_account_id, auth_role_id) VALUES (#{accountId}, #{roleId})")
    void insertRole(@Param("accountId") String accountId, @Param("roleId") int roleId);

    /** 幂等授予：已存在时不影响任何列，返回影响行数 0。 */
    @Insert("INSERT INTO auth_account_role (auth_account_id, auth_role_id) VALUES (#{accountId}, #{roleId}) "
            + "ON DUPLICATE KEY UPDATE granted_at = granted_at")
    int grantRoleIfAbsent(@Param("accountId") String accountId, @Param("roleId") int roleId);
}
