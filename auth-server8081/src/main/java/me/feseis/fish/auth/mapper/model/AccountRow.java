package me.feseis.fish.auth.mapper.model;

import lombok.Data;

/**
 * auth_account 查询行。不用 record：MyBatis 通过 getXxx 反射赋值。
 */
@Data
public class AccountRow {
    private String accountId;
    private String phone;
    private Integer status;
}
