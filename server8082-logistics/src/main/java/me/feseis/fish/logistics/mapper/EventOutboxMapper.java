package me.feseis.fish.logistics.mapper;

import java.time.LocalDateTime;
import java.util.List;

import me.feseis.fish.logistics.mapper.model.EventOutboxRow;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

public interface EventOutboxMapper {

    @Insert("INSERT INTO event_outbox (target_service, event_type, biz_type, biz_id, waybill_id) "
            + "VALUES (#{targetService}, #{eventType}, #{bizType}, #{bizId}, #{waybillId})")
    void insert(@Param("targetService") Integer targetService,
                @Param("eventType") Integer eventType,
                @Param("bizType") Integer bizType,
                @Param("bizId") String bizId,
                @Param("waybillId") String waybillId);

    /** 授予角色事件：带目标账号与角色。 */
    @Insert("INSERT INTO event_outbox (target_service, event_type, auth_account_id, auth_role_id, "
            + "biz_type, biz_id) VALUES ("
            + "#{targetService}, #{eventType}, #{authAccountId}, #{authRoleId}, #{bizType}, #{bizId})")
    void insertRoleGrant(@Param("targetService") Integer targetService,
                         @Param("eventType") Integer eventType,
                         @Param("authAccountId") String authAccountId,
                         @Param("authRoleId") Integer authRoleId,
                         @Param("bizType") Integer bizType,
                         @Param("bizId") String bizId);

    /**
     * 待投递与待重试事件。手动触发时忽略 next_retry_at，允许立即重试；
     * 接入定时任务后再按 next_retry_at 过滤。
     */
    @Select("SELECT * FROM event_outbox WHERE status IN (1, 3) AND target_service = #{targetService} "
            + "ORDER BY event_outbox_id LIMIT #{limit}")
    List<EventOutboxRow> findPending(@Param("targetService") Integer targetService,
                                     @Param("limit") int limit);

    @Update("UPDATE event_outbox SET status = 2, last_error = NULL, next_retry_at = NULL "
            + "WHERE event_outbox_id = #{eventOutboxId}")
    int markDelivered(@Param("eventOutboxId") Long eventOutboxId);

    @Update("UPDATE event_outbox SET status = 3, retry_count = #{retryCount}, "
            + "next_retry_at = #{nextRetryAt}, last_error = #{lastError} "
            + "WHERE event_outbox_id = #{eventOutboxId}")
    int markRetry(@Param("eventOutboxId") Long eventOutboxId,
                  @Param("retryCount") Integer retryCount,
                  @Param("nextRetryAt") LocalDateTime nextRetryAt,
                  @Param("lastError") String lastError);

    @Update("UPDATE event_outbox SET status = 4, retry_count = retry_count + 1, "
            + "next_retry_at = NULL, last_error = #{lastError} "
            + "WHERE event_outbox_id = #{eventOutboxId}")
    int markAbandoned(@Param("eventOutboxId") Long eventOutboxId,
                      @Param("lastError") String lastError);
}
