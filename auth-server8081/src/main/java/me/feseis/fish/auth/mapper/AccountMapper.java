package me.feseis.fish.auth.mapper;

import java.util.List;
import java.util.Optional;

import me.feseis.fish.auth.mapper.model.AccountRow;
import me.feseis.fish.auth.mapper.model.PasswordRow;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface AccountMapper {

    @Select("SELECT account_id, phone, status FROM auth_account WHERE phone = #{phone}")
    Optional<AccountRow> findByPhone(@Param("phone") String phone);

    @Select("SELECT status FROM auth_account WHERE account_id = #{accountId}")
    Optional<Integer> findStatus(@Param("accountId") String accountId);

    @Select("SELECT password_hash, pwd_version FROM auth_password WHERE account_id = #{accountId}")
    Optional<PasswordRow> findPassword(@Param("accountId") String accountId);

    @Select("SELECT role_id FROM auth_account_role WHERE account_id = #{accountId} ORDER BY role_id")
    List<Integer> findRoleIds(@Param("accountId") String accountId);

    @Insert("INSERT INTO auth_account (account_id, phone, status) VALUES (#{accountId}, #{phone}, 1)")
    void insertAccount(@Param("accountId") String accountId, @Param("phone") String phone);

    @Insert("INSERT INTO auth_password (account_id, password_hash, hash_algo, pwd_version) "
            + "VALUES (#{accountId}, #{passwordHash}, 'bcrypt', 0)")
    void insertPassword(@Param("accountId") String accountId, @Param("passwordHash") String passwordHash);

    @Insert("INSERT INTO auth_account_role (account_id, role_id) VALUES (#{accountId}, #{roleId})")
    void insertRole(@Param("accountId") String accountId, @Param("roleId") int roleId);
}
