package me.feseis.fish.payment.mapper;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import me.feseis.fish.payment.mapper.model.PaymentRow;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

public interface PaymentMapper {

    @Insert("INSERT INTO payment (payment_id, payment_no, biz_type, biz_id, payer_type, payer_account_id, "
            + "payer_phone, amount, pay_channel, status, pay_token, expire_at) VALUES ("
            + "#{paymentId}, #{paymentNo}, #{bizType}, #{bizId}, #{payerType}, #{payerAccountId}, "
            + "#{payerPhone}, #{amount}, #{payChannel}, #{status}, #{payToken}, #{expireAt})")
    void insert(PaymentRow row);

    @Select("SELECT * FROM payment WHERE payment_id = #{paymentId}")
    Optional<PaymentRow> findById(@Param("paymentId") String paymentId);

    @Select("SELECT * FROM payment WHERE payment_no = #{paymentNo}")
    Optional<PaymentRow> findByPaymentNo(@Param("paymentNo") String paymentNo);

    @Select("SELECT * FROM payment WHERE pay_channel = #{payChannel} AND channel_trade_no = #{tradeNo}")
    Optional<PaymentRow> findByChannelTradeNo(@Param("payChannel") Integer payChannel,
                                              @Param("tradeNo") String tradeNo);

    /** 未终结的单：待支付、已支付、超时待介入。用于建单幂等。 */
    @Select("SELECT * FROM payment WHERE biz_type = #{bizType} AND biz_id = #{bizId} "
            + "AND status IN (1, 2, 6) ORDER BY created_at DESC")
    List<PaymentRow> findActiveByBiz(@Param("bizType") Integer bizType,
                                     @Param("bizId") String bizId);

    /** 该业务下的全部支付单。业务终止时按状态分流，不受未终结条件限制。 */
    @Select("SELECT * FROM payment WHERE biz_type = #{bizType} AND biz_id = #{bizId} "
            + "ORDER BY created_at DESC")
    List<PaymentRow> findByBizAll(@Param("bizType") Integer bizType,
                                  @Param("bizId") String bizId);

    @Update("UPDATE payment SET channel_pay_url = #{url} WHERE payment_id = #{paymentId}")
    int updateChannelPayUrl(@Param("paymentId") String paymentId, @Param("url") String url);

    /** 模拟支付成功：渠道流水写 MOCK 前缀，保证与沙箱的数据形态一致。 */
    @Update("UPDATE payment SET status = 2, paid_at = #{paidAt}, "
            + "channel_trade_no = CONCAT('MOCK-T-', payment_no), "
            + "paid_biz_key = CONCAT(biz_type, ':', biz_id), "
            + "channel_pay_url = NULL, pay_token = NULL, next_remind_at = NULL "
            + "WHERE payment_id = #{paymentId} AND pay_channel = 1 AND status IN (1, 3, 6)")
    int mockPay(@Param("paymentId") String paymentId, @Param("paidAt") LocalDateTime paidAt);

    /** 重复支付：本单照常记已支付，但不写 paid_biz_key，等待人工退款。 */
    @Update("UPDATE payment SET status = 2, paid_at = #{paidAt}, "
            + "channel_trade_no = CONCAT('MOCK-T-', payment_no), "
            + "pay_remark = '重复支付，待人工退款', "
            + "channel_pay_url = NULL, pay_token = NULL, next_remind_at = NULL "
            + "WHERE payment_id = #{paymentId} AND status IN (1, 3, 6)")
    int markDuplicatePaid(@Param("paymentId") String paymentId, @Param("paidAt") LocalDateTime paidAt);

    /** 渠道确认支付成功。status 含 6 表示超时待介入的单也允许被渠道回调救回。 */
    @Update("UPDATE payment SET status = 2, paid_at = #{paidAt}, channel_trade_no = #{tradeNo}, "
            + "paid_biz_key = CONCAT(biz_type, ':', biz_id), "
            + "channel_pay_url = NULL, pay_token = NULL, next_remind_at = NULL "
            + "WHERE payment_id = #{paymentId} AND status IN (1, 3, 6)")
    int applyPaid(@Param("paymentId") String paymentId,
                  @Param("tradeNo") String tradeNo,
                  @Param("paidAt") LocalDateTime paidAt);

    /** 关闭后迟到支付：记账但不占 paid_biz_key，退款由 5-3 处理。 */
    @Update("UPDATE payment SET status = 2, paid_at = #{paidAt}, channel_trade_no = #{tradeNo}, "
            + "pay_remark = '关闭后迟到支付', "
            + "channel_pay_url = NULL, pay_token = NULL, next_remind_at = NULL "
            + "WHERE payment_id = #{paymentId} AND status = 4")
    int markLatePaid(@Param("paymentId") String paymentId,
                     @Param("tradeNo") String tradeNo,
                     @Param("paidAt") LocalDateTime paidAt);

    @Update("UPDATE payment SET status = 4, closed_at = #{closedAt}, pay_token = NULL, next_remind_at = NULL "
            + "WHERE payment_id = #{paymentId} AND status IN (1, 3)")
    int close(@Param("paymentId") String paymentId, @Param("closedAt") LocalDateTime closedAt);

    /** 全额退款完成。只允许从已支付流转，重复调用影响行数为 0。 */
    @Update("UPDATE payment SET status = 5 WHERE payment_id = #{paymentId} AND status = 2")
    int markRefunded(@Param("paymentId") String paymentId);
}
