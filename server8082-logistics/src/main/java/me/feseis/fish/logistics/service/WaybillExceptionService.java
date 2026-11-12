package me.feseis.fish.logistics.service;

import java.util.UUID;

import lombok.RequiredArgsConstructor;
import me.feseis.fish.logistics.vo.waybill.WaybillExceptionReqVO;
import me.feseis.fish.logistics.vo.waybill.WaybillOperationRespVO;
import me.feseis.fish.logistics.support.StaffGuard;
import me.feseis.fish.logistics.support.WaybillStatusGuard;
import me.feseis.fish.logistics.exp.BizException;
import me.feseis.fish.logistics.exp.ErrorCode;
import me.feseis.fish.logistics.mapper.WaybillExceptionMapper;
import me.feseis.fish.logistics.mapper.WaybillMapper;
import me.feseis.fish.logistics.mapper.model.StaffRow;
import me.feseis.fish.logistics.mapper.model.WaybillExceptionRow;
import me.feseis.fish.logistics.mapper.model.WaybillRow;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * 异常标记与解除。只允许在职物流员工操作，不走审核。
 * 解除即回到最近一条异常的 status_before。
 */
@Service
@RequiredArgsConstructor
public class WaybillExceptionService {

    private static final int STATUS_PENDING_PICKUP = 1;
    private static final int STATUS_PICKED = 2;
    private static final int STATUS_IN_TRANSIT = 3;
    private static final int STATUS_DELIVERING = 4;
    private static final int STATUS_EXCEPTION = 7;

    private static final int OP_STAFF = 2;
    private static final int TRACE_EXCEPTION = 7;
    private static final int TRACE_EXCEPTION_RESOLVED = 13;

    private final WaybillMapper waybillMapper;
    private final WaybillExceptionMapper waybillExceptionMapper;
    private final WaybillTraceService traceService;
    private final WaybillStatusGuard statusGuard;
    private final StaffGuard staffGuard;
    private final TransactionTemplate transactionTemplate;

    public WaybillOperationRespVO mark(String accountId, String waybillId, WaybillExceptionReqVO req) {
        StaffRow staff = staffGuard.requireOnDuty(accountId);
        return transactionTemplate.execute(tx -> {
            WaybillRow locked = waybillMapper.findByIdForUpdate(waybillId)
                    .orElseThrow(() -> BizException.of(ErrorCode.WAYBILL_NOT_FOUND));
            int current = locked.getStatus();
            statusGuard.require(current,
                    STATUS_PENDING_PICKUP, STATUS_PICKED, STATUS_IN_TRANSIT, STATUS_DELIVERING);

            WaybillExceptionRow row = new WaybillExceptionRow();
            row.setWaybillExceptionId(UUID.randomUUID().toString());
            row.setWaybillId(waybillId);
            row.setExceptionReasonId(req.exceptionReasonId());
            row.setExceptionRemark(req.exceptionRemark());
            row.setOperatorType(OP_STAFF);
            row.setOperatorId(staff.getLogisticsStaffId());
            row.setOperatorName(staff.getStaffName());
            row.setStatusBefore(current);
            waybillExceptionMapper.insert(row);

            int updated = waybillMapper.updateStatus(waybillId, current, STATUS_EXCEPTION);
            if (updated != 1) {
                throw BizException.of(ErrorCode.WAYBILL_STATUS_INVALID, "运单状态已变更，请刷新后重试");
            }
            traceService.write(waybillId, OP_STAFF, staff.getLogisticsStaffId(), staff.getStaffName(),
                    TRACE_EXCEPTION, STATUS_EXCEPTION);

            return new WaybillOperationRespVO(waybillId, locked.getWaybillNo(), STATUS_EXCEPTION);
        });
    }

    public WaybillOperationRespVO resolve(String accountId, String waybillId) {
        StaffRow staff = staffGuard.requireOnDuty(accountId);
        return transactionTemplate.execute(tx -> {
            WaybillRow locked = waybillMapper.findByIdForUpdate(waybillId)
                    .orElseThrow(() -> BizException.of(ErrorCode.WAYBILL_NOT_FOUND));
            statusGuard.require(locked.getStatus(), STATUS_EXCEPTION);

            WaybillExceptionRow latest = waybillExceptionMapper.findLatestByWaybillId(waybillId)
                    .orElseThrow(() -> BizException.of(ErrorCode.WAYBILL_STATUS_INVALID,
                            "异常状态缺少异常记录"));
            int origin = latest.getStatusBefore();

            int updated = waybillMapper.updateStatus(waybillId, STATUS_EXCEPTION, origin);
            if (updated != 1) {
                throw BizException.of(ErrorCode.WAYBILL_STATUS_INVALID, "运单状态已变更，请刷新后重试");
            }
            traceService.write(waybillId, OP_STAFF, staff.getLogisticsStaffId(), staff.getStaffName(),
                    TRACE_EXCEPTION_RESOLVED, origin);

            return new WaybillOperationRespVO(waybillId, locked.getWaybillNo(), origin);
        });
    }
}
