package me.feseis.fish.logistics.mapper.model;

import java.math.BigDecimal;

import lombok.Data;

/**
 * insurance_rule 查询行。
 */
@Data
public class InsuranceRuleRow {
    private Long insuranceRuleId;
    private BigDecimal rate;
    private BigDecimal minFee;
    private BigDecimal maxDeclaredValue;
}
