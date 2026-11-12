package me.feseis.fish.logistics.support;

import lombok.RequiredArgsConstructor;
import me.feseis.fish.logistics.exp.BizException;
import me.feseis.fish.logistics.exp.ErrorCode;
import me.feseis.fish.logistics.mapper.StaffSeqMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 员工工号：KD- + 8 位全局流水，不按日重置。必须在外层事务内调用。
 */
@Service
@RequiredArgsConstructor
public class StaffNoGenerator {

    private static final String PREFIX = "KD-";
    private static final long MAX_SEQ = 99_999_999L;

    private final StaffSeqMapper staffSeqMapper;

    @Transactional(propagation = Propagation.MANDATORY)
    public String next() {
        staffSeqMapper.nextSeq();
        long seq = staffSeqMapper.lastInsertId();
        if (seq <= 0 || seq > MAX_SEQ) {
            throw BizException.of(ErrorCode.INTERNAL, "工号序号超出范围：" + seq);
        }
        return PREFIX + String.format("%08d", seq);
    }
}
