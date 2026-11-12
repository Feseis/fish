package me.feseis.fish.logistics.mapper;

import java.time.LocalDate;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 对外单号日序号。取号必须在业务事务内执行，两条语句共用同一连接。
 */
public interface BizSeqMapper {

    @Insert("INSERT INTO biz_seq (biz, seq_date, current_seq) VALUES (#{biz}, #{seqDate}, LAST_INSERT_ID(1)) "
            + "ON DUPLICATE KEY UPDATE current_seq = LAST_INSERT_ID(current_seq + 1)")
    void nextSeq(@Param("biz") String biz, @Param("seqDate") LocalDate seqDate);

    @Select("SELECT LAST_INSERT_ID()")
    long lastInsertId();
}
