package com.feseis.fish.auth.store;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.ArrayList;
import java.util.List;

/**
 * 冷启动会话持久化：只存 refresh 令牌与账号摘要，不存 access。
 * 进程重启后由 TokenRefresher 用 refresh 换新 access。
 */
public final class PersistentSessionStore {

    private static final String PREFS_NAME = "fish_session";
    private static final String KEY_REFRESH_TOKEN = "refresh_token";
    private static final String KEY_ACCOUNT_ID = "account_id";
    private static final String KEY_PHONE = "phone";
    private static final String KEY_ROLES = "roles";

    private static SharedPreferences prefs;

    private PersistentSessionStore() {
    }

    public static void init(Context context) {
        prefs = context.getApplicationContext()
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public static void save(String accountId, String phone, List<Integer> roles,
                            String refreshToken) {
        if (prefs == null || refreshToken == null || refreshToken.isEmpty()) {
            return;
        }
        StringBuilder builder = new StringBuilder();
        if (roles != null) {
            for (Integer role : roles) {
                if (builder.length() > 0) {
                    builder.append(',');
                }
                builder.append(role);
            }
        }
        prefs.edit()
                .putString(KEY_REFRESH_TOKEN, refreshToken)
                .putString(KEY_ACCOUNT_ID, accountId == null ? "" : accountId)
                .putString(KEY_PHONE, phone == null ? "" : phone)
                .putString(KEY_ROLES, builder.toString())
                .apply();
    }

    public static boolean hasSession() {
        return !getRefreshToken().isEmpty();
    }

    public static String getRefreshToken() {
        return prefs == null ? "" : prefs.getString(KEY_REFRESH_TOKEN, "");
    }

    public static String getAccountId() {
        return prefs == null ? "" : prefs.getString(KEY_ACCOUNT_ID, "");
    }

    public static String getPhone() {
        return prefs == null ? "" : prefs.getString(KEY_PHONE, "");
    }

    public static List<Integer> getRoles() {
        List<Integer> roles = new ArrayList<>();
        if (prefs == null) {
            return roles;
        }
        String raw = prefs.getString(KEY_ROLES, "");
        if (raw.isEmpty()) {
            return roles;
        }
        for (String part : raw.split(",")) {
            try {
                roles.add(Integer.parseInt(part.trim()));
            } catch (NumberFormatException ignored) {
                // 脏数据跳过
            }
        }
        return roles;
    }

    public static void clear() {
        if (prefs != null) {
            prefs.edit().clear().apply();
        }
    }
}
