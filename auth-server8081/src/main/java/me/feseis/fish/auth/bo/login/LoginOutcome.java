package me.feseis.fish.auth.bo.login;

import java.time.Duration;

import me.feseis.fish.auth.dto.login.LoginResponse;

/**
 * 登录结果：响应体 + refresh 原值与有效期，供 Controller 组装 Set-Cookie。
 */
public record LoginOutcome(LoginResponse response, String refreshToken, Duration refreshTtl) {
}
