package me.feseis.fish.payment.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.feseis.fish.payment.api.dto.BizTerminatedReqDTO;
import me.feseis.fish.payment.api.dto.BizTerminatedRespDTO;
import me.feseis.fish.payment.mapper.PaymentMapper;
import me.feseis.fish.payment.mapper.PaymentOutboxMapper;
import me.feseis.fish.payment.mapper.RefundMapper;
import me.feseis.fish.payment.mapper.model.PaymentRow;
import me.feseis.fish.payment.mapper.model.RefundRow;
import me.feseis.fish.payment.support.PaymentNoGenerator;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 业务终止结算：运单取消或退回时，关闭该运单下未支付的支付单，已支付单建全额退款。
 * 幂等由 refund.uk_payment 兜底，事件重复投递不会产生第二笔退款。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentSettlementService {

    private static final int BIZ_TYPE_WAYBILL = 1;
    private static final int BIZ_TYPE_WAYBILL_CANCEL = 2;

    private static final int STATUS_PENDING = 1;
    private static final int STATUS_PAID = 2;
    private static final int STATUS_FAILED = 3;

    private static final int CHANNEL_MOCK = 1;

    private static final int REFUND_PENDING = 1;
    private static final int REFUND_SOURCE_CANCEL = 1;
    private static final int REFUND_SOURCE_MANUAL = 3;
    private static final int REFUND_SOURCE_RETURN = 4;

    private static final int OUTBOX_TARGET_LOGISTICS = 2;
    private static final int OUTBOX_REFUND_DONE = 2;

    private final PaymentMapper paymentMapper;
    private final RefundMapper refundMapper;
    private final PaymentOutboxMapper paymentOutboxMapper;
    private final PaymentNoGenerator paymentNoGenerator;

    @Transactional
    public BizTerminatedRespDTO settle(BizTerminatedReqDTO req) {
        List<PaymentRow> rows = paymentMapper.findByBizAll(BIZ_TYPE_WAYBILL, req.waybillId());
        LocalDateTime now = LocalDateTime.now();
        int closed = 0;
        int refunded = 0;
        int skipped = 0;

        for (PaymentRow row : rows) {
            int status = row.getStatus();
            if (status == STATUS_PENDING || status == STATUS_FAILED) {
                closed += paymentMapper.close(row.getPaymentId(), now);
            } else if (status == STATUS_PAID) {
                if (createRefund(row, req, now)) {
                    refunded++;
                } else {
                    skipped++;
                }
            } else {
                skipped++;
            }
        }
        return new BizTerminatedRespDTO(closed, refunded, skipped);
    }

    /** @return true 表示本次新建了退款单；已有退款单或并发撞唯一键返回 false。 */
    private boolean createRefund(PaymentRow payment, BizTerminatedReqDTO req, LocalDateTime now) {
        if (refundMapper.findByPaymentId(payment.getPaymentId()).isPresent()) {
            return false;
        }

        RefundRow refund = new RefundRow();
        refund.setRefundId(UUID.randomUUID().toString());
        refund.setRefundNo(paymentNoGenerator.nextRefundNo());
        refund.setPaymentId(payment.getPaymentId());
        refund.setAmount(payment.getAmount());
        refund.setRefundSource(refundSourceOf(req.bizType()));
        refund.setReason(refundReasonOf(payment, req.bizType()));
        refund.setStatus(REFUND_PENDING);
        try {
            refundMapper.insert(refund);
        } catch (DuplicateKeyException e) {
            return false;
        }

        if (!Integer.valueOf(CHANNEL_MOCK).equals(payment.getPayChannel())) {
            // 支付宝退款渠道未接入，退款单停在待退款，等待人工或后续补偿任务
            log.warn("渠道退款未接入，退款单待处理 paymentId={} refundNo={}",
                    payment.getPaymentId(), refund.getRefundNo());
            return true;
        }

        refundMapper.markRefunded(refund.getRefundId(), "MOCK-R-" + refund.getRefundNo(), now);
        paymentMapper.markRefunded(payment.getPaymentId());
        paymentOutboxMapper.insert(OUTBOX_TARGET_LOGISTICS, OUTBOX_REFUND_DONE,
                payment.getBizType(), payment.getBizId(), payment.getPaymentId(), refund.getRefundId());
        return true;
    }

    private static int refundSourceOf(int bizType) {
        if (bizType == BIZ_TYPE_WAYBILL_CANCEL) {
            return REFUND_SOURCE_CANCEL;
        }
        if (bizType == BIZ_TYPE_WAYBILL) {
            return REFUND_SOURCE_RETURN;
        }
        return REFUND_SOURCE_MANUAL;
    }

    private static String refundReasonOf(PaymentRow payment, int bizType) {
        if (payment.getPaidBizKey() == null) {
            return "重复支付退款";
        }
        return bizType == BIZ_TYPE_WAYBILL_CANCEL ? "运单取消" : "运单退回";
    }
}
