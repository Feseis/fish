package me.feseis.fish.payment.support;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

import lombok.RequiredArgsConstructor;
import me.feseis.fish.payment.exp.BizException;
import me.feseis.fish.payment.exp.ErrorCode;
import me.feseis.fish.payment.mapper.BizSeqMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 支付单号：P + yyMMdd + 8 位日序号。必须在外层事务内调用。
 */
@Service
@RequiredArgsConstructor
public class PaymentNoGenerator {

    private static final String BIZ = "PAYMENT";
    private static final String PREFIX = "P";
    private static final String BIZ_REFUND = "REFUND";
    private static final String PREFIX_REFUND = "R";
    private static final long MAX_SEQ = 99_999_999L;
    private static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyMMdd");

    private final BizSeqMapper bizSeqMapper;

    @Transactional(propagation = Propagation.MANDATORY)
    public String next() {
        LocalDate today = LocalDate.now(ZONE);
        bizSeqMapper.nextSeq(BIZ, today);
        long seq = bizSeqMapper.lastInsertId();
        if (seq <= 0 || seq > MAX_SEQ) {
            throw BizException.of(ErrorCode.INTERNAL, "支付单号序号超出范围：" + seq);
        }
        return PREFIX + today.format(DATE_FORMAT) + String.format("%08d", seq);
    }

    /** 退款单号：R + yyMMdd + 8 位日序号。必须在外层事务内调用。 */
    @Transactional(propagation = Propagation.MANDATORY)
    public String nextRefundNo() {
        LocalDate today = LocalDate.now(ZONE);
        bizSeqMapper.nextSeq(BIZ_REFUND, today);
        long seq = bizSeqMapper.lastInsertId();
        if (seq <= 0 || seq > MAX_SEQ) {
            throw BizException.of(ErrorCode.INTERNAL, "退款单号序号超出范围：" + seq);
        }
        return PREFIX_REFUND + today.format(DATE_FORMAT) + String.format("%08d", seq);
    }
}
