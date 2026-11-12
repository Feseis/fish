package me.feseis.fish.auth.controller;

import lombok.extern.slf4j.Slf4j;
import me.feseis.fish.auth.api.dto.AuditDecisionRespDTO;
import me.feseis.fish.auth.api.dto.StaffApplyAuditReqDTO;
import me.feseis.fish.auth.api.dto.WaybillCancelAuditReqDTO;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 内部审核占位接口。按业务分路径，当前一律返回通过，不查库、不校验入参内容。
 * 后续替换为真实人工审核时只改本类实现，调用方无需变更。
 */
@Slf4j
@RestController
@RequestMapping("/auth/internal/audit")
public class InternalAuditController {

    @PostMapping("/waybill-cancel")
    public AuditDecisionRespDTO auditWaybillCancel(
            @RequestBody(required = false) WaybillCancelAuditReqDTO req) {
        log.info("占位审核：运单取消 bizId={}", req == null ? null : req.waybillCancelId());
        return passThrough();
    }

    @PostMapping("/staff-apply")
    public AuditDecisionRespDTO auditStaffApply(
            @RequestBody(required = false) StaffApplyAuditReqDTO req) {
        log.info("占位审核：员工入职 bizId={}", req == null ? null : req.staffApplyId());
        return passThrough();
    }

    private static AuditDecisionRespDTO passThrough() {
        return new AuditDecisionRespDTO(true, 1, "系统审核", "占位接口，固定通过");
    }
}
