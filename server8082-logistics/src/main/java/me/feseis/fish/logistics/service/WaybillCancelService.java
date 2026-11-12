package me.feseis.fish.logistics.service;

import java.time.LocalDateTime;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import me.feseis.fish.logistics.client.AuthClient;
import me.feseis.fish.logistics.support.StaffGuard;
import me.feseis.fish.logistics.support.WaybillStatusGuard;
import me.feseis.fish.logistics.vo.waybill.WaybillCancelReqVO;
import me.feseis.fish.logistics.vo.waybill.WaybillOperationRespVO;
import me.feseis.fish.logistics.exp.BizException;
import me.feseis.fish.logistics.exp.ErrorCode;
import me.feseis.fish.logistics.mapper.ApprovalMapper;
import me.feseis.fish.logistics.mapper.EventOutboxMapper;
import me.feseis.fish.logistics.mapper.WaybillCancelMapper;
import me.feseis.fish.logistics.mapper.WaybillExceptionMapper;
import me.feseis.fish.logistics.mapper.WaybillMapper;
import me.feseis.fish.logistics.mapper.WaybillReturnMapper;
import me.feseis.fish.logistics.mapper.model.ApprovalRow;
import me.feseis.fish.logistics.mapper.model.StaffRow;
import me.feseis.fish.logistics.mapper.model.WaybillCancelRow;
import me.feseis.fish.logistics.mapper.model.WaybillExceptionRow;
import me.feseis.fish.logistics.mapper.model.WaybillRow;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * 运单取消。分流规则见运单表草稿：
 * 1待揽收 / 2已揽收 → 6已取消；3运输中 → 8退回中并写退回单；
 * 7异常按最近一条异常的 status_before 分流。用户取消已揽收及以后需走占位审核，员工取消不审核。
 */
@Service
@RequiredArgsConstructor
public class WaybillCancelService {

    private static final int STATUS_PENDING_PICKUP = 1;
    private static final int STATUS_PICKED = 2;
    private static final int STATUS_IN_TRANSIT = 3;
    private static final int STATUS_DELIVERING = 4;
    private static final int STATUS_CANCELLED = 6;
    private static final int STATUS_EXCEPTION = 7;
    private static final int STATUS_RETURNING = 8;

    private static final int OP_SYSTEM = 1;
    private static final int OP_STAFF = 2;
    private static final int OP_USER = 4;

    private static final int TRACE_CANCEL = 8;
    private static final int RETURN_REASON_CANCEL = 7;

    private static final int APPROVAL_PASSED = 2;
    private static final int BIZ_TYPE_CANCEL = 2;
    private static final int EVENT_TARGET_PAYMENT = 3;
    private static final int EVENT_TYPE_BIZ_TERMINATED = 3;

    private final WaybillMapper waybillMapper;
    private final WaybillCancelMapper waybillCancelMapper;
    private final ApprovalMapper approvalMapper;
    private final WaybillExceptionMapper waybillExceptionMapper;
    private final WaybillReturnMapper waybillReturnMapper;
    private final EventOutboxMapper eventOutboxMapper;
    private final WaybillTraceService traceService;
    private final WaybillStatusGuard statusGuard;
    private final StaffGuard staffGuard;
    private final AuthClient authClient;
    private final TransactionTemplate transactionTemplate;

    public WaybillOperationRespVO cancel(String accountId, String waybillId, WaybillCancelReqVO req) {
        WaybillRow waybill = waybillMapper.findById(waybillId)
                .orElseThrow(() -> BizException.of(ErrorCode.WAYBILL_NOT_FOUND));

        StaffRow staff = staffGuard.findOnDuty(accountId).orElse(null);
        boolean isOwner = accountId.equals(waybill.getAuthAccountId());
        if (staff == null && !isOwner) {
            throw BizException.of(ErrorCode.WAYBILL_NOT_FOUND);
        }

        int currentStatus = waybill.getStatus();
        statusGuard.require(currentStatus,
                STATUS_PENDING_PICKUP, STATUS_PICKED, STATUS_IN_TRANSIT, STATUS_EXCEPTION);

        int operatorType = staff != null ? OP_STAFF : OP_USER;
        String operatorId = staff != null ? staff.getLogisticsStaffId() : accountId;
        String operatorName = staff != null ? staff.getStaffName() : waybill.getSenderName();

        String cancelId = UUID.randomUUID().toString();
        if (staff == null && currentStatus != STATUS_PENDING_PICKUP) {
            authClient.auditWaybillCancel(cancelId, waybillId, req.cancelReasonId(),
                    currentStatus, operatorType, operatorId);
        }

        return transactionTemplate.execute(tx -> doCancel(cancelId, waybillId, currentStatus,
                req, operatorType, operatorId, operatorName));
    }

    private WaybillOperationRespVO doCancel(String cancelId,
                                              String waybillId,
                                              int expectedStatus,
                                              WaybillCancelReqVO req,
                                              int operatorType,
                                              String operatorId,
                                              String operatorName) {
        WaybillRow locked = waybillMapper.findByIdForUpdate(waybillId)
                .orElseThrow(() -> BizException.of(ErrorCode.WAYBILL_NOT_FOUND));
        if (!Integer.valueOf(expectedStatus).equals(locked.getStatus())) {
            throw BizException.of(ErrorCode.WAYBILL_STATUS_INVALID, "运单状态已变更，请刷新后重试");
        }

        CancelTarget target = resolveTarget(waybillId, expectedStatus);
        LocalDateTime now = LocalDateTime.now();

        WaybillCancelRow cancel = new WaybillCancelRow();
        cancel.setWaybillCancelId(cancelId);
        cancel.setWaybillId(waybillId);
        cancel.setApplyRound(waybillCancelMapper.countByWaybillId(waybillId) + 1);
        cancel.setCancelReasonId(req.cancelReasonId());
        cancel.setCancelRemark(req.cancelRemark());
        cancel.setOperatorType(operatorType);
        cancel.setOperatorId(operatorId);
        cancel.setOperatorName(operatorName);
        cancel.setStatusBefore(expectedStatus);
        cancel.setCancelledAt(now);
        waybillCancelMapper.insert(cancel);

        ApprovalRow approval = new ApprovalRow();
        approval.setApprovalId(UUID.randomUUID().toString());
        approval.setBizType(BIZ_TYPE_CANCEL);
        approval.setBizId(cancelId);
        approval.setStatus(APPROVAL_PASSED);
        approval.setApplyOperatorType(operatorType);
        approval.setApplyOperatorId(operatorId);
        approval.setApplyOperatorName(operatorName);
        approval.setApplyRemark(req.cancelRemark());
        approval.setAuditOperatorType(OP_SYSTEM);
        approval.setAuditOperatorName("系统审核");
        approval.setAuditRemark("占位接口自动通过");
        approval.setAuditedAt(now);
        approvalMapper.insert(approval);

        if (target.returnStatusBefore() != null) {
            waybillReturnMapper.insert(UUID.randomUUID().toString(), waybillId, RETURN_REASON_CANCEL,
                    "运单取消联动退回", operatorType, operatorId, operatorName,
                    target.returnStatusBefore());
        }

        int updated = waybillMapper.updateStatus(waybillId, expectedStatus, target.status());
        if (updated != 1) {
            throw BizException.of(ErrorCode.WAYBILL_STATUS_INVALID, "运单状态已变更，请刷新后重试");
        }

        traceService.write(waybillId, operatorType, operatorId, operatorName,
                TRACE_CANCEL, target.status());

        eventOutboxMapper.insert(EVENT_TARGET_PAYMENT, EVENT_TYPE_BIZ_TERMINATED,
                BIZ_TYPE_CANCEL, cancelId, waybillId);

        return new WaybillOperationRespVO(waybillId, locked.getWaybillNo(), target.status());
    }

    private CancelTarget resolveTarget(String waybillId, int currentStatus) {
        if (currentStatus == STATUS_EXCEPTION) {
            WaybillExceptionRow latest = waybillExceptionMapper.findLatestByWaybillId(waybillId)
                    .orElseThrow(() -> BizException.of(ErrorCode.WAYBILL_STATUS_INVALID,
                            "异常状态缺少异常记录"));
            int origin = latest.getStatusBefore();
            if (origin == STATUS_PENDING_PICKUP || origin == STATUS_PICKED) {
                return new CancelTarget(STATUS_CANCELLED, null);
            }
            if (origin == STATUS_IN_TRANSIT || origin == STATUS_DELIVERING) {
                return new CancelTarget(STATUS_RETURNING, origin);
            }
            throw BizException.of(ErrorCode.WAYBILL_STATUS_INVALID, "当前异常来源不支持取消");
        }
        if (currentStatus == STATUS_PENDING_PICKUP || currentStatus == STATUS_PICKED) {
            return new CancelTarget(STATUS_CANCELLED, null);
        }
        return new CancelTarget(STATUS_RETURNING, STATUS_IN_TRANSIT);
    }

    /** status 为目标状态；returnStatusBefore 非空表示需写退回单，值为退回单的 status_before。 */
    private record CancelTarget(int status, Integer returnStatusBefore) {
    }
}
