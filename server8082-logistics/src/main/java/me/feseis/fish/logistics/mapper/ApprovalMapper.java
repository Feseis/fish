package me.feseis.fish.logistics.mapper;

import me.feseis.fish.logistics.mapper.model.ApprovalRow;
import org.apache.ibatis.annotations.Insert;

public interface ApprovalMapper {

    @Insert("INSERT INTO approval (approval_id, biz_type, biz_id, status, "
            + "apply_operator_type, apply_operator_id, apply_operator_name, apply_remark, "
            + "audit_operator_type, audit_operator_id, audit_operator_name, audit_remark, audited_at) VALUES ("
            + "#{approvalId}, #{bizType}, #{bizId}, #{status}, "
            + "#{applyOperatorType}, #{applyOperatorId}, #{applyOperatorName}, #{applyRemark}, "
            + "#{auditOperatorType}, #{auditOperatorId}, #{auditOperatorName}, #{auditRemark}, #{auditedAt})")
    void insert(ApprovalRow row);
}
