package me.feseis.fish.auth.mapper;

import java.time.LocalDateTime;
import java.util.Optional;

import me.feseis.fish.auth.mapper.model.SessionRow;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

public interface SessionMapper {

    @Insert("""
            INSERT INTO auth_session
              (session_id, account_id, refresh_hash, pwd_version, status, login_at, last_active_at, expire_at)
            VALUES
              (#{sessionId}, #{accountId}, #{refreshHash}, #{pwdVersion}, 1, #{loginAt}, #{loginAt}, #{expireAt})
            """)
    void insert(@Param("sessionId") String sessionId,
                @Param("accountId") String accountId,
                @Param("refreshHash") String refreshHash,
                @Param("pwdVersion") int pwdVersion,
                @Param("loginAt") LocalDateTime loginAt,
                @Param("expireAt") LocalDateTime expireAt);

    @Select("""
            SELECT session_id, account_id, pwd_version, status, expire_at
            FROM auth_session
            WHERE refresh_hash = #{refreshHash}
            """)
    Optional<SessionRow> findByRefreshHash(@Param("refreshHash") String refreshHash);

    @Update("UPDATE auth_session SET last_active_at = #{now} WHERE session_id = #{sessionId}")
    void touch(@Param("sessionId") String sessionId, @Param("now") LocalDateTime now);

    /** 结束会话：status 2 登出 / 3 被踢 / 4 过期，同时记 logout_at。 */
    @Update("""
            UPDATE auth_session
            SET status = #{status}, logout_at = #{now}
            WHERE session_id = #{sessionId}
            """)
    void close(@Param("sessionId") String sessionId,
               @Param("status") int status,
               @Param("now") LocalDateTime now);
}
