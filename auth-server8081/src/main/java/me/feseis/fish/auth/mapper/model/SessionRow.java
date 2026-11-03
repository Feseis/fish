package me.feseis.fish.auth.mapper.model;

import java.time.LocalDateTime;

import lombok.Data;

/**
 * auth_session 查询行：refresh 续期只需要会话主键、账号、密码版本快照与生命周期字段。
 */
@Data
public class SessionRow {
    private String sessionId;
    private String accountId;
    private Integer pwdVersion;
    private Integer status;
    private LocalDateTime expireAt;
}
