package me.feseis.fish.logistics.service;

import java.util.List;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import me.feseis.fish.logistics.client.PaymentClient;
import me.feseis.fish.logistics.support.StaffGuard;
import me.feseis.fish.logistics.support.WaybillNoGenerator;
import me.feseis.fish.logistics.vo.price.PriceQuoteReqVO;
import me.feseis.fish.logistics.vo.price.PriceQuoteRespVO;
import me.feseis.fish.logistics.vo.waybill.WaybillCreateReqVO;
import me.feseis.fish.logistics.vo.waybill.WaybillDetailRespVO;
import me.feseis.fish.logistics.vo.waybill.WaybillCreateRespVO;
import me.feseis.fish.logistics.vo.waybill.WaybillTraceRespVO;
import me.feseis.fish.logistics.exp.BizException;
import me.feseis.fish.logistics.exp.ErrorCode;
import me.feseis.fish.logistics.mapper.RoutingMapper;
import me.feseis.fish.logistics.mapper.WaybillMapper;
import me.feseis.fish.logistics.mapper.model.WaybillRow;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 下单：超区校验 → 计费 → 生成运单号 → 落库。不写支付单，不写首条轨迹。
 */
@Service
@RequiredArgsConstructor
public class WaybillService {

    private static final int STATUS_PENDING_PICKUP = 1;
    private static final int MAX_PAGE_SIZE = 50;

    private static final int PAY_METHOD_PREPAID = 1;
    private static final int PAY_CHANNEL_MOCK = 1;
    private static final int BIZ_TYPE_WAYBILL = 1;

    private final RoutingMapper routingMapper;
    private final WaybillMapper waybillMapper;
    private final PriceQuoteService priceQuoteService;
    private final WaybillNoGenerator waybillNoGenerator;
    private final StaffGuard staffGuard;
    private final WaybillTraceService waybillTraceService;
    private final PaymentClient paymentClient;

    @Transactional
    public WaybillCreateRespVO create(String accountId, WaybillCreateReqVO req) {
        routingMapper.findCoveringSiteId(req.sender().adcode())
                .orElseThrow(() -> BizException.of(ErrorCode.DELIVERY_AREA_UNCOVERED,
                        "寄件地址 " + req.sender().adcode() + " 暂无网点覆盖"));
        routingMapper.findCoveringSiteId(req.receiver().adcode())
                .orElseThrow(() -> BizException.of(ErrorCode.DELIVERY_AREA_UNCOVERED,
                        "收件地址 " + req.receiver().adcode() + " 暂无网点覆盖"));

        PriceQuoteRespVO quote = priceQuoteService.quote(new PriceQuoteReqVO(
                req.sender().adcode(), req.receiver().adcode(),
                req.itemWeight(), req.declaredValue()));

        String waybillId = UUID.randomUUID().toString();
        String waybillNo = waybillNoGenerator.next();

        WaybillRow row = new WaybillRow();
        row.setWaybillId(waybillId);
        row.setWaybillNo(waybillNo);
        row.setAuthAccountId(accountId);

        row.setSenderName(req.sender().name());
        row.setSenderPhone(req.sender().phone());
        row.setSenderProvince(req.sender().province());
        row.setSenderCity(req.sender().city());
        row.setSenderDistrict(req.sender().district());
        row.setSenderDetailAddr(req.sender().detailAddr());
        row.setSenderAdcode(req.sender().adcode());
        row.setSenderPoiId(req.sender().poiId());
        row.setSenderPoiName(req.sender().poiName());
        row.setSenderLng(req.sender().lng());
        row.setSenderLat(req.sender().lat());

        row.setReceiverName(req.receiver().name());
        row.setReceiverPhone(req.receiver().phone());
        row.setReceiverProvince(req.receiver().province());
        row.setReceiverCity(req.receiver().city());
        row.setReceiverDistrict(req.receiver().district());
        row.setReceiverDetailAddr(req.receiver().detailAddr());
        row.setReceiverAdcode(req.receiver().adcode());
        row.setReceiverPoiId(req.receiver().poiId());
        row.setReceiverPoiName(req.receiver().poiName());
        row.setReceiverLng(req.receiver().lng());
        row.setReceiverLat(req.receiver().lat());

        row.setItemName(req.itemName());
        row.setItemCategoryId(req.itemCategoryId());
        row.setItemWeight(req.itemWeight());
        row.setDeclaredValue(req.declaredValue());

        row.setFreightAmount(quote.freightAmount());
        row.setInsuredFee(quote.insuredFee());
        row.setTotalAmount(quote.totalAmount());
        row.setPriceRuleId(quote.priceRuleId());
        row.setInsuranceRuleId(quote.insuranceRuleId());
        row.setPayMethod(req.payMethod());

        row.setStatus(STATUS_PENDING_PICKUP);
        row.setRemark(req.remark());

        waybillMapper.insert(row);

        if (Integer.valueOf(PAY_METHOD_PREPAID).equals(req.payMethod())) {
            int payChannel = req.payChannel() == null ? PAY_CHANNEL_MOCK : req.payChannel();
            paymentClient.createOrder(BIZ_TYPE_WAYBILL, waybillId, quote.totalAmount(),
                    payChannel, PAY_METHOD_PREPAID, accountId, req.sender().phone());
        }

        WaybillRow saved = waybillMapper.findOwned(waybillId, accountId)
                .orElseThrow(() -> BizException.of(ErrorCode.WAYBILL_NOT_FOUND));

        return new WaybillCreateRespVO(
                waybillId,
                waybillNo,
                STATUS_PENDING_PICKUP,
                quote.freightRelationId(),
                quote.priceRuleId(),
                quote.insuranceRuleId(),
                quote.freightAmount(),
                quote.insuredFee(),
                quote.totalAmount(),
                saved.getCreatedAt());
    }

    public WaybillDetailRespVO detail(String accountId, String waybillId) {
        WaybillRow row = waybillMapper.findOwned(waybillId, accountId)
                .orElseThrow(() -> BizException.of(ErrorCode.WAYBILL_NOT_FOUND));
        return toDetail(row);
    }

    public List<WaybillDetailRespVO> list(String accountId, int page, int size) {
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        int safePage = Math.max(page, 1);
        return waybillMapper.listByAccount(accountId, (safePage - 1) * safeSize, safeSize)
                .stream()
                .map(WaybillService::toDetail)
                .toList();
    }

    /** 轨迹查询：运单归属人或在职员工可见。 */
    public List<WaybillTraceRespVO> trace(String accountId, String waybillId) {
        WaybillRow row = waybillMapper.findById(waybillId)
                .orElseThrow(() -> BizException.of(ErrorCode.WAYBILL_NOT_FOUND));
        if (!accountId.equals(row.getAuthAccountId()) && !staffGuard.isOnDuty(accountId)) {
            throw BizException.of(ErrorCode.WAYBILL_NOT_FOUND);
        }
        return waybillTraceService.list(waybillId).stream()
                .map(t -> new WaybillTraceRespVO(
                        t.getWaybillTraceId(),
                        t.getTraceActionId(),
                        t.getStatusAfter(),
                        t.getOperatorType(),
                        t.getOperatorName(),
                        t.getOccurredAt()))
                .toList();
    }

    private static WaybillDetailRespVO toDetail(WaybillRow row) {
        return new WaybillDetailRespVO(
                row.getWaybillId(),
                row.getWaybillNo(),
                row.getStatus(),
                new WaybillDetailRespVO.AddressView(
                        row.getSenderName(), row.getSenderPhone(), row.getSenderAdcode(),
                        row.getSenderProvince(), row.getSenderCity(), row.getSenderDistrict(),
                        row.getSenderDetailAddr(), row.getSenderPoiName(),
                        row.getSenderLng(), row.getSenderLat()),
                new WaybillDetailRespVO.AddressView(
                        row.getReceiverName(), row.getReceiverPhone(), row.getReceiverAdcode(),
                        row.getReceiverProvince(), row.getReceiverCity(), row.getReceiverDistrict(),
                        row.getReceiverDetailAddr(), row.getReceiverPoiName(),
                        row.getReceiverLng(), row.getReceiverLat()),
                row.getItemName(),
                row.getItemCategoryId(),
                row.getItemWeight(),
                row.getDeclaredValue(),
                row.getFreightAmount(),
                row.getInsuredFee(),
                row.getTotalAmount(),
                row.getPayMethod(),
                row.getRemark(),
                row.getPickedAt(),
                row.getDeliveredAt(),
                row.getCreatedAt());
    }
}
