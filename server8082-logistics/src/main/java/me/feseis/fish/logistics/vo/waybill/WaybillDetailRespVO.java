package me.feseis.fish.logistics.vo.waybill;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 运单详情，寄收件为下单时的快照。
 */
public record WaybillDetailRespVO(
        String waybillId,
        String waybillNo,
        Integer status,
        AddressView sender,
        AddressView receiver,
        String itemName,
        Integer itemCategoryId,
        BigDecimal itemWeight,
        BigDecimal declaredValue,
        BigDecimal freightAmount,
        BigDecimal insuredFee,
        BigDecimal totalAmount,
        Integer payMethod,
        String remark,
        LocalDateTime pickedAt,
        LocalDateTime deliveredAt,
        LocalDateTime createdAt
) {

    public record AddressView(
            String name,
            String phone,
            String adcode,
            String province,
            String city,
            String district,
            String detailAddr,
            String poiName,
            BigDecimal lng,
            BigDecimal lat
    ) {
    }
}
