package com.feseis.fish.auth.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 对应后端 LoginResponse；accessExpiresAt 保留后端返回的字符串，避免客户端做时区换算。
 */
public final class LoginResult {

    private final String accountId;
    private final String phone;
    private final List<Integer> roles;
    private final String accessToken;
    private final String accessExpiresAt;

    public LoginResult(String accountId, String phone, List<Integer> roles,
                       String accessToken, String accessExpiresAt) {
        this.accountId = accountId;
        this.phone = phone;
        this.roles = roles == null
                ? Collections.<Integer>emptyList()
                : Collections.unmodifiableList(new ArrayList<>(roles));
        this.accessToken = accessToken;
        this.accessExpiresAt = accessExpiresAt;
    }

    public String getAccountId() {
        return this.accountId;
    }

    public String getPhone() {
        return this.phone;
    }

    public List<Integer> getRoles() {
        return this.roles;
    }

    public String getAccessToken() {
        return this.accessToken;
    }

    public String getAccessExpiresAt() {
        return this.accessExpiresAt;
    }
}
