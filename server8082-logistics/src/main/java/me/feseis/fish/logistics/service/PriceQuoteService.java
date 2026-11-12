package me.feseis.fish.logistics.service;

import java.math.BigDecimal;
import java.math.RoundingMode;

import lombok.RequiredArgsConstructor;
import me.feseis.fish.logistics.vo.price.PriceQuoteReqVO;
import me.feseis.fish.logistics.vo.price.PriceQuoteRespVO;
import me.feseis.fish.logistics.exp.BizException;
import me.feseis.fish.logistics.exp.ErrorCode;
import me.feseis.fish.logistics.mapper.PricingMapper;
import me.feseis.fish.logistics.mapper.model.InsuranceRuleRow;
import me.feseis.fish.logistics.mapper.model.PriceRuleRow;
import me.feseis.fish.logistics.support.FreightRelationResolver;
import org.springframework.stereotype.Service;

/**
 * 运费与保价费试算。只读，不落库；下单时再调一次并以落库结果为准。
 */
@Service
@RequiredArgsConstructor
public class PriceQuoteService {

    private static final BigDecimal ZERO = new BigDecimal("0.00");

    private final PricingMapper pricingMapper;

    public PriceQuoteRespVO quote(PriceQuoteReqVO req) {
        int relationId = FreightRelationResolver.resolve(req.senderAdcode(), req.receiverAdcode());

        PriceRuleRow priceRule = pricingMapper.findEnabledPriceRule(relationId)
                .orElseThrow(() -> BizException.of(ErrorCode.PRICE_RULE_MISSING,
                        "freight_relation_id=" + relationId));

        BigDecimal freight = calcFreight(priceRule, req.itemWeight());

        Long insuranceRuleId = null;
        BigDecimal insuredFee = ZERO;
        if (req.declaredValue() != null && req.declaredValue().compareTo(BigDecimal.ZERO) > 0) {
            InsuranceRuleRow insuranceRule = pricingMapper.findEnabledInsuranceRule()
                    .orElseThrow(() -> BizException.of(ErrorCode.INSURANCE_RULE_MISSING));
            if (insuranceRule.getMaxDeclaredValue() != null
                    && req.declaredValue().compareTo(insuranceRule.getMaxDeclaredValue()) > 0) {
                throw BizException.of(ErrorCode.INSURANCE_LIMIT_EXCEEDED,
                        "上限 " + insuranceRule.getMaxDeclaredValue().toPlainString() + " 元");
            }
            insuredFee = req.declaredValue()
                    .multiply(insuranceRule.getRate())
                    .max(insuranceRule.getMinFee())
                    .setScale(2, RoundingMode.HALF_UP);
            insuranceRuleId = insuranceRule.getInsuranceRuleId();
        }

        BigDecimal total = freight.add(insuredFee).setScale(2, RoundingMode.HALF_UP);

        return new PriceQuoteRespVO(
                relationId,
                priceRule.getPriceRuleId(),
                insuranceRuleId,
                freight,
                insuredFee,
                total);
    }

    private BigDecimal calcFreight(PriceRuleRow rule, BigDecimal weight) {
        if (weight.compareTo(rule.getFirstWeight()) <= 0) {
            return rule.getFirstPrice().setScale(2, RoundingMode.HALF_UP);
        }
        BigDecimal overWeight = weight.subtract(rule.getFirstWeight());
        BigDecimal units = overWeight.divide(rule.getExtraUnit(), 0, RoundingMode.CEILING);
        BigDecimal extra = units.multiply(rule.getExtraPrice());
        return rule.getFirstPrice().add(extra).setScale(2, RoundingMode.HALF_UP);
    }
}
