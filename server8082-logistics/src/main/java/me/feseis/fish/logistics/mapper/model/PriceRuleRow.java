package me.feseis.fish.logistics.mapper.model;

import java.math.BigDecimal;

import lombok.Data;

/**
 * price_rule 查询行。不直接用 record：MyBatis 通过 getXxx 反射赋值。
 */
@Data
public class PriceRuleRow {
    private Long priceRuleId;
    private Integer freightRelationId;
    private BigDecimal firstWeight;
    private BigDecimal firstPrice;
    private BigDecimal extraUnit;
    private BigDecimal extraPrice;
}
