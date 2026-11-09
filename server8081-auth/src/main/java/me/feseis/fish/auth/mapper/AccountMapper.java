package me.feseis.fish.auth.mapper;

import java.util.List;
import java.util.Optional;

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

    @Select("SELECT password_hash, pwd_version FROM auth_password WHERE auth_account_id = #{accountId}")
    Optional<PasswordRow> findPassword(@Param("accountId") String accountId);

    @Select("SELECT auth_role_id AS role_id FROM auth_account_role WHERE auth_account_id = #{accountId} ORDER BY auth_role_id")
    List<Integer> findRoleIds(@Param("accountId") String accountId);

    @Insert("INSERT INTO auth_account (auth_account_id, phone, status) VALUES (#{accountId}, #{phone}, 1)")
    void insertAccount(@Param("accountId") String accountId, @Param("phone") String phone);

    @Insert("INSERT INTO auth_user_profile (auth_user_profile_id, auth_account_id) "
            + "VALUES (#{profileId}, #{accountId})")
    void insertProfile(@Param("profileId") String profileId, @Param("accountId") String accountId);

    @Insert("INSERT INTO auth_password (auth_password_id, auth_account_id, password_hash, hash_algo, pwd_version) "
            + "VALUES (#{passwordId}, #{accountId}, #{passwordHash}, 'bcrypt', 0)")
    void insertPassword(@Param("passwordId") String passwordId,
                        @Param("accountId") String accountId,
                        @Param("passwordHash") String passwordHash);

    @Insert("INSERT INTO auth_account_role (auth_account_id, auth_role_id) VALUES (#{accountId}, #{roleId})")
    void insertRole(@Param("accountId") String accountId, @Param("roleId") int roleId);
}
