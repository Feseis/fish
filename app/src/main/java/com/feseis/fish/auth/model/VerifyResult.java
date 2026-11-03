package com.feseis.fish.auth.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 对应后端 VerifyResponse：下游凭此获得调用者身份与角色。
 */
public final class VerifyResult {

    private final String sessionId;
    private final String accountId;
    private final List<Integer> roles;

    public VerifyResult(String sessionId, String accountId, List<Integer> roles) {
        this.sessionId = sessionId;
        this.accountId = accountId;
        this.roles = roles == null
                ? Collections.<Integer>emptyList()
                : Collections.unmodifiableList(new ArrayList<>(roles));
    }

    public String getSessionId() {
        return this.sessionId;
    }

    public String getAccountId() {
        return this.accountId;
    }

    public List<Integer> getRoles() {
        return this.roles;
    }
}
