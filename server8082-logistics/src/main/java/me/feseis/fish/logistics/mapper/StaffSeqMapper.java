package me.feseis.fish.logistics.mapper;

import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * 员工工号全局序列。取号必须在业务事务内执行，两条语句共用同一连接。
 */
public interface StaffSeqMapper {

    @Update("UPDATE staff_seq SET current_seq = LAST_INSERT_ID(current_seq + 1) WHERE seq_name = 'STAFF'")
    int nextSeq();

    @Select("SELECT LAST_INSERT_ID()")
    long lastInsertId();
}
