package me.feseis.fish.logistics.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import me.feseis.fish.logistics.routing.RouteResolver;
import me.feseis.fish.logistics.routing.RoutingSnapshot;
import me.feseis.fish.logistics.support.HandoverNoGenerator;
import me.feseis.fish.logistics.support.StaffGuard;
import me.feseis.fish.logistics.support.WaybillStatusGuard;
import me.feseis.fish.logistics.vo.handover.HandoverCreateReqVO;
import me.feseis.fish.logistics.vo.handover.HandoverLoadReqVO;
import me.feseis.fish.logistics.vo.handover.HandoverRespVO;
import me.feseis.fish.logistics.exp.BizException;
import me.feseis.fish.logistics.exp.ErrorCode;
import me.feseis.fish.logistics.mapper.HandoverMapper;
import me.feseis.fish.logistics.mapper.RoutingMapper;
import me.feseis.fish.logistics.mapper.WaybillMapper;
import me.feseis.fish.logistics.mapper.model.HandoverRow;
import me.feseis.fish.logistics.mapper.model.StaffRow;
import me.feseis.fish.logistics.mapper.model.TransportLineRow;
import me.feseis.fish.logistics.mapper.model.WaybillRow;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 交接单（班次）：创建待发 → 分批扫码装车 → 发车 → 到达。
 * 装车时按路由解析校验「这票货是否该上这条线路」，调用方不选线路。
 */
@Service
@RequiredArgsConstructor
public class HandoverService {

    private static final int DIRECTION_FORWARD = 1;
    private static final int STATUS_PENDING = 0;
    private static final int STATUS_DEPARTED = 1;
    private static final int STATUS_ARRIVED = 2;

    private static final int WAYBILL_PICKED = 2;
    private static final int WAYBILL_IN_TRANSIT = 3;
    private static final int WAYBILL_DELIVERING = 4;

    private static final int OP_STAFF = 2;
    private static final int TRACE_ARRIVE = 2;
    private static final int TRACE_LEAVE = 3;
    private static final int TRACE_DELIVERING = 4;

    private final HandoverMapper handoverMapper;
    private final WaybillMapper waybillMapper;
    private final RoutingMapper routingMapper;
    private final RouteResolver routeResolver;
    private final HandoverNoGenerator handoverNoGenerator;
    private final WaybillTraceService traceService;
    private final WaybillStatusGuard statusGuard;
    private final StaffGuard staffGuard;

    @Transactional
    public HandoverRespVO create(String accountId, HandoverCreateReqVO req) {
        StaffRow staff = staffGuard.requireOnDuty(accountId);
        TransportLineRow line = routingMapper.findEnabledLineById(req.transportLineId())
                .orElseThrow(() -> BizException.of(ErrorCode.NOT_FOUND, "运输线路不存在或已停用"));

        HandoverRow row = new HandoverRow();
        row.setHandoverId(UUID.randomUUID().toString());
        row.setHandoverNo(handoverNoGenerator.next());
        row.setTransportLineId(line.getTransportLineId());
        row.setDirection(DIRECTION_FORWARD);
        row.setFromSiteId(line.getOriginSiteId());
        row.setToSiteId(line.getDestSiteId());
        row.setVehicleId(req.vehicleId());
        row.setVehiclePlate(null);
        row.setDriverStaffId(req.driverStaffId());
        row.setDriverName(null);
        row.setOperatorName(staff.getStaffName());
        row.setDepartedAt(null);
        row.setArrivedAt(null);
        row.setStatus(STATUS_PENDING);
        handoverMapper.insert(row);

        return toResponse(row, List.of());
    }

    @Transactional
    public HandoverRespVO load(String accountId, String handoverId, HandoverLoadReqVO req) {
        StaffRow staff = staffGuard.requireOnDuty(accountId);
        HandoverRow handover = handoverMapper.findByIdForUpdate(handoverId)
                .orElseThrow(() -> BizException.of(ErrorCode.HANDOVER_NOT_FOUND));
        if (!Integer.valueOf(STATUS_PENDING).equals(handover.getStatus())) {
            throw BizException.of(ErrorCode.HANDOVER_STATUS_INVALID, "交接单已发车或已到达");
        }

        RoutingSnapshot snapshot = routeResolver.loadSnapshot();
        LocalDateTime now = LocalDateTime.now();

        for (String waybillId : req.waybillIds()) {
            if (handoverMapper.countItem(handoverId, waybillId) > 0) {
                continue;
            }
            WaybillRow waybill = waybillMapper.findByIdForUpdate(waybillId)
                    .orElseThrow(() -> BizException.of(ErrorCode.WAYBILL_NOT_FOUND));
            statusGuard.require(waybill.getStatus(), WAYBILL_PICKED);

            if (!handover.getFromSiteId().equals(waybill.getCurrentSiteId())) {
                throw BizException.of(ErrorCode.WAYBILL_STATUS_INVALID,
                        "运单 " + waybill.getWaybillNo() + " 不在交出网点");
            }

            TransportLineRow next = routeResolver
                    .next(snapshot, waybill.getCurrentSiteId(), waybill.getReceiverAdcode())
                    .orElseThrow(() -> BizException.of(ErrorCode.ROUTE_MISSING,
                            "运单 " + waybill.getWaybillNo() + " 没有可用的下一跳线路"));
            if (!next.getTransportLineId().equals(handover.getTransportLineId())) {
                throw BizException.of(ErrorCode.WAYBILL_STATUS_INVALID,
                        "运单 " + waybill.getWaybillNo() + " 应走线路 " + next.getLineCode());
            }

            handoverMapper.insertItem(UUID.randomUUID().toString(), handoverId, waybillId, now);
            int updated = waybillMapper.updateStatus(waybillId, WAYBILL_PICKED, WAYBILL_IN_TRANSIT);
            if (updated != 1) {
                throw BizException.of(ErrorCode.WAYBILL_STATUS_INVALID, "运单状态已变更，请刷新后重试");
            }
            traceService.write(waybillId, OP_STAFF, staff.getLogisticsStaffId(), staff.getStaffName(),
                    TRACE_LEAVE, WAYBILL_IN_TRANSIT);
        }

        return detail(handoverId);
    }

    @Transactional
    public HandoverRespVO depart(String accountId, String handoverId) {
        staffGuard.requireOnDuty(accountId);
        handoverMapper.findByIdForUpdate(handoverId)
                .orElseThrow(() -> BizException.of(ErrorCode.HANDOVER_NOT_FOUND));
        if (handoverMapper.listItemWaybillIds(handoverId).isEmpty()) {
            throw BizException.of(ErrorCode.HANDOVER_STATUS_INVALID, "交接单没有装车运单");
        }
        int updated = handoverMapper.markDeparted(handoverId, LocalDateTime.now());
        if (updated != 1) {
            throw BizException.of(ErrorCode.HANDOVER_STATUS_INVALID, "交接单当前状态不允许发车");
        }
        return detail(handoverId);
    }

    @Transactional
    public HandoverRespVO arrive(String accountId, String handoverId) {
        StaffRow staff = staffGuard.requireOnDuty(accountId);
        HandoverRow handover = handoverMapper.findByIdForUpdate(handoverId)
                .orElseThrow(() -> BizException.of(ErrorCode.HANDOVER_NOT_FOUND));
        int updated = handoverMapper.markArrived(handoverId, LocalDateTime.now());
        if (updated != 1) {
            throw BizException.of(ErrorCode.HANDOVER_STATUS_INVALID, "交接单当前状态不允许到达");
        }

        for (String waybillId : handoverMapper.listItemWaybillIds(handoverId)) {
            WaybillRow waybill = waybillMapper.findByIdForUpdate(waybillId)
                    .orElseThrow(() -> BizException.of(ErrorCode.WAYBILL_NOT_FOUND));
            if (!Integer.valueOf(WAYBILL_IN_TRANSIT).equals(waybill.getStatus())) {
                continue;
            }
            waybillMapper.updateCurrentSite(waybillId, handover.getToSiteId());
            traceService.write(waybillId, OP_STAFF, staff.getLogisticsStaffId(), staff.getStaffName(),
                    TRACE_ARRIVE, WAYBILL_IN_TRANSIT);

            Optional<String> covering = routingMapper.findCoveringSiteId(waybill.getReceiverAdcode());
            if (covering.isPresent() && covering.get().equals(handover.getToSiteId())) {
                int moved = waybillMapper.updateStatus(waybillId, WAYBILL_IN_TRANSIT, WAYBILL_DELIVERING);
                if (moved == 1) {
                    traceService.write(waybillId, OP_STAFF, staff.getLogisticsStaffId(),
                            staff.getStaffName(), TRACE_DELIVERING, WAYBILL_DELIVERING);
                }
            }
        }

        return detail(handoverId);
    }

    @Transactional(readOnly = true)
    public HandoverRespVO detail(String handoverId) {
        HandoverRow handover = handoverMapper.findById(handoverId)
                .orElseThrow(() -> BizException.of(ErrorCode.HANDOVER_NOT_FOUND));
        return toResponse(handover, handoverMapper.listItemWaybillIds(handoverId));
    }

    private static HandoverRespVO toResponse(HandoverRow row, List<String> waybillIds) {
        return new HandoverRespVO(
                row.getHandoverId(),
                row.getHandoverNo(),
                row.getTransportLineId(),
                row.getDirection(),
                row.getFromSiteId(),
                row.getToSiteId(),
                row.getVehiclePlate(),
                row.getDriverName(),
                row.getDepartedAt(),
                row.getArrivedAt(),
                row.getStatus(),
                waybillIds);
    }
}
