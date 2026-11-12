package me.feseis.fish.payment.mapper;

import java.time.LocalDateTime;
import java.util.Optional;

import me.feseis.fish.payment.mapper.model.RefundRow;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

public interface RefundMapper {

    @Insert("INSERT INTO refund (refund_id, refund_no, payment_id, amount, refund_source, reason, status) "
            + "VALUES (#{refundId}, #{refundNo}, #{paymentId}, #{amount}, #{refundSource}, #{reason}, #{status})")
    void insert(RefundRow row);

    @Select("SELECT * FROM refund WHERE payment_id = #{paymentId}")
    Optional<RefundRow> findByPaymentId(@Param("paymentId") String paymentId);

    @Select("SELECT * FROM refund WHERE refund_id = #{refundId}")
    Optional<RefundRow> findById(@Param("refundId") String refundId);

    /** 退款成功。条件更新保证重复回调只生效一次。 */
    @Update("UPDATE refund SET status = 2, channel_refund_no = #{channelRefundNo}, "
            + "refunded_at = #{refundedAt}, refund_remark = NULL "
            + "WHERE refund_id = #{refundId} AND status = 1")
    int markRefunded(@Param("refundId") String refundId,
                     @Param("channelRefundNo") String channelRefundNo,
                     @Param("refundedAt") LocalDateTime refundedAt);
}
