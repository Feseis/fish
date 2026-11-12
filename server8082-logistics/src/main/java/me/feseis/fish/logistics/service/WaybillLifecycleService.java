package me.feseis.fish.logistics.service;

import java.time.LocalDateTime;

import lombok.RequiredArgsConstructor;
import me.feseis.fish.logistics.support.StaffGuard;
import me.feseis.fish.logistics.support.WaybillStatusGuard;
import me.feseis.fish.logistics.vo.waybill.WaybillDeliverReqVO;
import me.feseis.fish.logistics.vo.waybill.WaybillOperationRespVO;
import me.feseis.fish.logistics.exp.BizException;
import me.feseis.fish.logistics.exp.ErrorCode;
import me.feseis.fish.logistics.mapper.RoutingMapper;
import me.feseis.fish.logistics.mapper.WaybillMapper;
import me.feseis.fish.logistics.mapper.model.StaffRow;
import me.feseis.fish.logistics.mapper.model.WaybillRow;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 运单末端动作：揽收、签收、投递失败。全部要求在职员工。
 */
@Service
@RequiredArgsConstructor
public class WaybillLifecycleService {

    private static final int STATUS_PENDING_PICKUP = 1;
    private static final int STATUS_PICKED = 2;
    private static final int STATUS_DELIVERING = 4;
    private static final int STATUS_DELIVERED = 5;

    private static final int OP_STAFF = 2;
    private static final int TRACE_PICKUP = 1;
    private static final int TRACE_DELIVER_FAIL = 5;
    private static final int TRACE_SIGN_SELF = 6;
    private static final int TRACE_SIGN_OTHER = 11;

    private final WaybillMapper waybillMapper;
    private final RoutingMapper routingMapper;
    private final WaybillTraceService traceService;
    private final WaybillStatusGuard statusGuard;
    private final StaffGuard staffGuard;

    @Transactional
    public WaybillOperationRespVO pickup(String accountId, String waybillId) {
        StaffRow staff = staffGuard.requireOnDuty(accountId);
        WaybillRow waybill = waybillMapper.findByIdForUpdate(waybillId)
                .orElseThrow(() -> BizException.of(ErrorCode.WAYBILL_NOT_FOUND));
        statusGuard.require(waybill.getStatus(), STATUS_PENDING_PICKUP);

        String siteId = routingMapper.findCoveringSiteId(waybill.getSenderAdcode())
                .orElseThrow(() -> BizException.of(ErrorCode.DELIVERY_AREA_UNCOVERED,
                        "寄件地址暂无网点覆盖"));
        if (!siteId.equals(staff.getLogisticsSiteId())) {
            throw BizException.of(ErrorCode.STAFF_FORBIDDEN, "该运单不属于你的网点");
        }

        int updated = waybillMapper.markPicked(waybillId, siteId, LocalDateTime.now());
        if (updated != 1) {
            throw BizException.of(ErrorCode.WAYBILL_STATUS_INVALID, "运单状态已变更，请刷新后重试");
        }
        traceService.write(waybillId, OP_STAFF, staff.getLogisticsStaffId(), staff.getStaffName(),
                TRACE_PICKUP, STATUS_PICKED);

        return new WaybillOperationRespVO(waybillId, waybill.getWaybillNo(), STATUS_PICKED);
    }

    @Transactional
    public WaybillOperationRespVO deliver(String accountId, String waybillId, WaybillDeliverReqVO req) {
        StaffRow staff = staffGuard.requireOnDuty(accountId);
        WaybillRow waybill = waybillMapper.findByIdForUpdate(waybillId)
                .orElseThrow(() -> BizException.of(ErrorCode.WAYBILL_NOT_FOUND));
        statusGuard.require(waybill.getStatus(), STATUS_DELIVERING);

        int signType = req.signType();
        if (signType == 2 && (isBlank(req.signName()) || isBlank(req.signPhone()))) {
            throw BizException.of(ErrorCode.INVALID_PARAM, "他人代收必须填写姓名与手机号");
        }

        int updated = waybillMapper.markDelivered(waybillId, STATUS_DELIVERING, signType,
                req.signName(), req.signPhone(), LocalDateTime.now());
        if (updated != 1) {
            throw BizException.of(ErrorCode.WAYBILL_STATUS_INVALID, "运单状态已变更，请刷新后重试");
        }

        int action = signType == 2 ? TRACE_SIGN_OTHER : TRACE_SIGN_SELF;
        traceService.write(waybillId, OP_STAFF, staff.getLogisticsStaffId(), staff.getStaffName(),
                action, STATUS_DELIVERED);

        return new WaybillOperationRespVO(waybillId, waybill.getWaybillNo(), STATUS_DELIVERED);
    }

    /** 投递失败不改变状态，只留轨迹；多次失败后由员工改用异常标记。 */
    @Transactional
    public WaybillOperationRespVO deliverFail(String accountId, String waybillId) {
        StaffRow staff = staffGuard.requireOnDuty(accountId);
        WaybillRow waybill = waybillMapper.findById(waybillId)
                .orElseThrow(() -> BizException.of(ErrorCode.WAYBILL_NOT_FOUND));
        statusGuard.require(waybill.getStatus(), STATUS_DELIVERING);

        traceService.write(waybillId, OP_STAFF, staff.getLogisticsStaffId(), staff.getStaffName(),
                TRACE_DELIVER_FAIL, STATUS_DELIVERING);

        return new WaybillOperationRespVO(waybillId, waybill.getWaybillNo(), STATUS_DELIVERING);
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
