package me.feseis.fish.logistics.dto.price;

import java.math.BigDecimal;

/**
 * 运费试算出参。金额单位元，保留 2 位小数。
 */
public record PriceQuoteResponse(
        Integer freightRelationId,
        Long priceRuleId,
        Long insuranceRuleId,
        BigDecimal freightAmount,
        BigDecimal insuredFee,
        BigDecimal totalAmount
) {
}
