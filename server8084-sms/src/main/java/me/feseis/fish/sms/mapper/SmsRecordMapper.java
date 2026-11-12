package me.feseis.fish.sms.mapper;

import java.util.Optional;

import me.feseis.fish.sms.mapper.model.SmsRecordRow;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface SmsRecordMapper {

    @Options(useGeneratedKeys = true, keyProperty = "smsRecordId")
    @Insert("INSERT INTO sms_record (request_id, template_code, target_phone, content, source_service, "
            + "biz_type, biz_id, result, fail_reason) VALUES ("
            + "#{requestId}, #{templateCode}, #{targetPhone}, #{content}, #{sourceService}, "
            + "#{bizType}, #{bizId}, #{result}, #{failReason})")
    void insert(SmsRecordRow row);

    @Select("SELECT * FROM sms_record WHERE request_id = #{requestId}")
    Optional<SmsRecordRow> findByRequestId(@Param("requestId") String requestId);
}
