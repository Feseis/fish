package me.feseis.fish.payment.mapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;

/**
 * 支付服务事件投递表。当前只写入不投递，消费方接口与定时投递后续补。
 */
public interface PaymentOutboxMapper {

    @Insert("INSERT INTO payment_outbox (target_service, event_type, biz_type, biz_id, payment_id, refund_id) "
            + "VALUES (#{targetService}, #{eventType}, #{bizType}, #{bizId}, #{paymentId}, #{refundId})")
    void insert(@Param("targetService") Integer targetService,
                @Param("eventType") Integer eventType,
                @Param("bizType") Integer bizType,
                @Param("bizId") String bizId,
                @Param("paymentId") String paymentId,
                @Param("refundId") String refundId);
}
