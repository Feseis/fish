package me.feseis.fish.logistics.mapper;

import java.util.Optional;

import me.feseis.fish.logistics.mapper.model.InsuranceRuleRow;
import me.feseis.fish.logistics.mapper.model.PriceRuleRow;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface PricingMapper {

    @Select("SELECT price_rule_id, freight_relation_id, first_weight, first_price, extra_unit, extra_price "
            + "FROM price_rule WHERE freight_relation_id = #{relationId} AND enabled = 1 LIMIT 1")
    Optional<PriceRuleRow> findEnabledPriceRule(@Param("relationId") int relationId);

    @Select("SELECT insurance_rule_id, rate, min_fee, max_declared_value "
            + "FROM insurance_rule WHERE enabled = 1 LIMIT 1")
    Optional<InsuranceRuleRow> findEnabledInsuranceRule();
}
