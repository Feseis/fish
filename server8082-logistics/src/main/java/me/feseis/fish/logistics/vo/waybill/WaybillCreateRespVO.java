package me.feseis.fish.logistics.vo.waybill;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 下单结果。金额为下单时算定的快照。
 */
public record WaybillCreateRespVO(
        String waybillId,
        String waybillNo,
        Integer status,
        Integer freightRelationId,
        Long priceRuleId,
        Long insuranceRuleId,
        BigDecimal freightAmount,
        BigDecimal insuredFee,
        BigDecimal totalAmount,
        LocalDateTime createdAt
) {
}
