package me.feseis.fish.auth.mapper.model;

import lombok.Data;

/**
 * auth_password 查询行：哈希与版本号必须一次取出，会话表要存版本快照。
 */
@Data
public class PasswordRow {
    private String passwordHash;
    private Integer pwdVersion;
}
