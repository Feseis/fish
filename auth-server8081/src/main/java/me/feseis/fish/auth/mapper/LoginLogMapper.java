package me.feseis.fish.auth.mapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;

public interface LoginLogMapper {

    @Insert("""
            INSERT INTO auth_login_log
              (account_id, phone, login_type, result, fail_reason, session_id, client_ip)
            VALUES
              (#{accountId}, #{phone}, 'PASSWORD', #{result}, #{failReason}, #{sessionId}, #{clientIp})
            """)
    void insert(@Param("accountId") String accountId,
                @Param("phone") String phone,
                @Param("result") int result,
                @Param("failReason") String failReason,
                @Param("sessionId") String sessionId,
                @Param("clientIp") String clientIp);
}
