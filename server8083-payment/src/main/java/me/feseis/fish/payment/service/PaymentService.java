package me.feseis.fish.payment.service;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import me.feseis.fish.payment.api.dto.PaymentCreateReqDTO;
import me.feseis.fish.payment.support.PaymentNoGenerator;
import me.feseis.fish.payment.vo.PaymentRespVO;
import me.feseis.fish.payment.exp.BizException;
import me.feseis.fish.payment.exp.ErrorCode;
import me.feseis.fish.payment.mapper.PaymentMapper;
import me.feseis.fish.payment.mapper.model.PaymentRow;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 支付单建单、查询、模拟支付、渠道查单、关闭。
 * 模拟渠道与支付宝沙箱共用同一套状态机与字段口径。
 */
@Service
@RequiredArgsConstructor
public class PaymentService {

    private static final int CHANNEL_MOCK = 1;
    private static final int CHANNEL_ALIPAY = 2;
    private static final int STATUS_PENDING = 1;
    private static final int STATUS_PAID = 2;

    private static final int PAY_METHOD_COLLECT = 2;

    private static final Duration PREPAID_TTL = Duration.ofMinutes(30);
    private static final Duration COLLECT_TTL = Duration.ofHours(48);

    private static final SecureRandom RANDOM = new SecureRandom();

    private final PaymentMapper paymentMapper;
    private final PaymentNoGenerator paymentNoGenerator;
    private final AlipayPaymentService alipayPaymentService;

    @Transactional
    public PaymentRespVO create(PaymentCreateReqDTO req) {
        if (req.amount() == null || req.amount().signum() <= 0) {
            throw BizException.of(ErrorCode.PAYMENT_AMOUNT_INVALID);
        }
        validateChannel(req.payChannel());

        List<PaymentRow> active = paymentMapper.findActiveByBiz(req.bizType(), req.bizId());
        if (!active.isEmpty()) {
            return toResponse(active.get(0));
        }

        PaymentRow row = new PaymentRow();
        row.setPaymentId(UUID.randomUUID().toString());
        row.setPaymentNo(paymentNoGenerator.next());
        row.setBizType(req.bizType());
        row.setBizId(req.bizId());

        boolean anonymous = req.payerAccountId() == null || req.payerAccountId().isBlank();
        row.setPayerType(anonymous ? 2 : 1);
        row.setPayerAccountId(anonymous ? null : req.payerAccountId());
        row.setPayerPhone(req.payerPhone());

        row.setAmount(req.amount());
        row.setPayChannel(req.payChannel());
        row.setStatus(STATUS_PENDING);
        row.setPayToken(anonymous ? newToken() : null);
        row.setExpireAt(LocalDateTime.now().plus(ttl(req.payMethod())));

        paymentMapper.insert(row);

        if (Integer.valueOf(CHANNEL_ALIPAY).equals(req.payChannel())) {
            alipayPaymentService.precreate(row);
        }

        return toResponse(paymentMapper.findById(row.getPaymentId())
                .orElseThrow(() -> BizException.of(ErrorCode.PAYMENT_NOT_FOUND)));
    }

    @Transactional(readOnly = true)
    public PaymentRespVO findById(String paymentId) {
        return toResponse(paymentMapper.findById(paymentId)
                .orElseThrow(() -> BizException.of(ErrorCode.PAYMENT_NOT_FOUND)));
    }

    @Transactional(readOnly = true)
    public List<PaymentRespVO> findByBiz(Integer bizType, String bizId) {
        return paymentMapper.findActiveByBiz(bizType, bizId).stream()
                .map(PaymentService::toResponse)
                .toList();
    }

    /** 模拟支付成功。同业务已有成功单时，本单记为重复支付，不占用 paid_biz_key。 */
    @Transactional
    public PaymentRespVO mockPay(String paymentId) {
        PaymentRow row = paymentMapper.findById(paymentId)
                .orElseThrow(() -> BizException.of(ErrorCode.PAYMENT_NOT_FOUND));
        if (!Integer.valueOf(CHANNEL_MOCK).equals(row.getPayChannel())) {
            throw BizException.of(ErrorCode.CHANNEL_NOT_SUPPORTED, "该单不是模拟支付渠道");
        }

        LocalDateTime now = LocalDateTime.now();
        boolean alreadyPaid = paymentMapper.findActiveByBiz(row.getBizType(), row.getBizId()).stream()
                .anyMatch(item -> Integer.valueOf(STATUS_PAID).equals(item.getStatus())
                        && item.getPaidBizKey() != null
                        && !item.getPaymentId().equals(paymentId));

        int updated = alreadyPaid
                ? paymentMapper.markDuplicatePaid(paymentId, now)
                : paymentMapper.mockPay(paymentId, now);
        if (updated != 1) {
            throw BizException.of(ErrorCode.PAYMENT_STATUS_INVALID, "支付单当前状态不允许支付");
        }
        return toResponse(paymentMapper.findById(paymentId)
                .orElseThrow(() -> BizException.of(ErrorCode.PAYMENT_NOT_FOUND)));
    }

    /** 支付宝主动查单。渠道未配置时直接拒绝。 */
    @Transactional
    public PaymentRespVO syncFromChannel(String paymentId) {
        PaymentRow row = paymentMapper.findById(paymentId)
                .orElseThrow(() -> BizException.of(ErrorCode.PAYMENT_NOT_FOUND));
        if (!Integer.valueOf(CHANNEL_ALIPAY).equals(row.getPayChannel())) {
            throw BizException.of(ErrorCode.CHANNEL_NOT_SUPPORTED, "该单不是支付宝渠道");
        }
        validateChannel(CHANNEL_ALIPAY);
        alipayPaymentService.sync(row);
        return toResponse(paymentMapper.findById(paymentId)
                .orElseThrow(() -> BizException.of(ErrorCode.PAYMENT_NOT_FOUND)));
    }

    @Transactional
    public PaymentRespVO close(String paymentId) {
        int updated = paymentMapper.close(paymentId, LocalDateTime.now());
        if (updated != 1) {
            throw BizException.of(ErrorCode.PAYMENT_STATUS_INVALID, "支付单当前状态不允许关闭");
        }
        return toResponse(paymentMapper.findById(paymentId)
                .orElseThrow(() -> BizException.of(ErrorCode.PAYMENT_NOT_FOUND)));
    }

    private void validateChannel(Integer payChannel) {
        if (Integer.valueOf(CHANNEL_MOCK).equals(payChannel)) {
            return;
        }
        if (Integer.valueOf(CHANNEL_ALIPAY).equals(payChannel)) {
            if (!alipayPaymentService.isConfigured()) {
                throw BizException.of(ErrorCode.CHANNEL_NOT_SUPPORTED, "支付宝沙箱未配置");
            }
            return;
        }
        throw BizException.of(ErrorCode.CHANNEL_NOT_SUPPORTED);
    }

    private static Duration ttl(Integer payMethod) {
        return Integer.valueOf(PAY_METHOD_COLLECT).equals(payMethod) ? COLLECT_TTL : PREPAID_TTL;
    }

    private static String newToken() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static PaymentRespVO toResponse(PaymentRow row) {
        String qrCode = Integer.valueOf(STATUS_PENDING).equals(row.getStatus())
                ? row.getChannelPayUrl()
                : null;
        return new PaymentRespVO(
                row.getPaymentId(),
                row.getPaymentNo(),
                row.getBizType(),
                row.getBizId(),
                row.getAmount(),
                row.getPayChannel(),
                row.getStatus(),
                row.getPayToken(),
                qrCode,
                row.getPayRemark(),
                row.getExpireAt(),
                row.getPaidAt(),
                row.getClosedAt(),
                row.getCreatedAt());
    }
}
