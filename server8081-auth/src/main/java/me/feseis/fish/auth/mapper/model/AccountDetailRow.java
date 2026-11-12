package me.feseis.fish.auth.mapper.model;

import lombok.Data;

/**
 * 账号 + 资料联表查询行。realName 是该账号唯一权威姓名。
 */
@Data
public class AccountDetailRow {
    private String accountId;
    private String phone;
    private Integer status;
    private String realName;
}
