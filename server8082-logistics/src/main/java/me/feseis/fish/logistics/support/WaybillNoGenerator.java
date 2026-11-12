package me.feseis.fish.logistics.support;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

import lombok.RequiredArgsConstructor;
import me.feseis.fish.logistics.exp.BizException;
import me.feseis.fish.logistics.exp.ErrorCode;
import me.feseis.fish.logistics.mapper.BizSeqMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 运单号：FF + yyMMdd + 6 位日序号。必须在外层事务内调用，保证取号与插入同成同败。
 */
@Service
@RequiredArgsConstructor
public class WaybillNoGenerator {

    private static final String BIZ = "WAYBILL";
    private static final String PREFIX = "FF";
    private static final int MAX_SEQ = 999_999;
    private static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyMMdd");

    private final BizSeqMapper bizSeqMapper;

    @Transactional(propagation = Propagation.MANDATORY)
    public String next() {
        LocalDate today = LocalDate.now(ZONE);
        bizSeqMapper.nextSeq(BIZ, today);
        long seq = bizSeqMapper.lastInsertId();
        if (seq <= 0 || seq > MAX_SEQ) {
            throw BizException.of(ErrorCode.INTERNAL, "运单号序号超出范围：" + seq);
        }
        return PREFIX + today.format(DATE_FORMAT) + String.format("%06d", seq);
    }
}
