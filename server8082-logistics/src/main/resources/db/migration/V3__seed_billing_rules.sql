-- V3__seed_billing_rules.sql
-- 计费规则种子数据：三档运费 + 一条保价规则。

INSERT INTO price_rule
  (price_rule_id, freight_relation_id, first_weight, first_price, extra_unit, extra_price, enabled)
VALUES
  (1, 1, 1.000, 8.00,  0.500, 2.00, 1),
  (2, 2, 1.000, 10.00, 0.500, 3.00, 1),
  (3, 3, 1.000, 12.00, 0.500, 5.00, 1);

INSERT INTO insurance_rule
  (insurance_rule_id, rate, min_fee, max_declared_value, enabled)
VALUES
  (1, 0.0050, 1.00, NULL, 1);
