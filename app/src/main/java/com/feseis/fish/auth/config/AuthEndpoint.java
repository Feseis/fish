package com.feseis.fish.auth.config;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;

/**
 * 认证服务地址。开发调试阶段由用户在首页手动填写，无默认值；地址变化对每次请求即时生效。
 */
public final class AuthEndpoint {

    private static final String PREFS_NAME = "fish_prefs";
    private static final String KEY_BASE_URL = "auth_base_url";

    private AuthEndpoint() {
    }

    public static String get(Context context) {
        return normalize(prefs(context).getString(KEY_BASE_URL, ""));
    }

    public static void save(Context context, String raw) {
        prefs(context).edit().putString(KEY_BASE_URL, normalize(raw)).apply();
    }

    /**
     * 去掉首尾空白；缺少 scheme 时补 http://；去掉尾部斜杠。
     */
    public static String normalize(String raw) {
        if (raw == null) {
            return "";
        }
        String value = raw.trim();
        if (value.isEmpty()) {
            return "";
        }
        if (!value.startsWith("http://") && !value.startsWith("https://")) {
            value = "http://" + value;
        }
        while (value.endsWith("/")) {
            value = value.substring(0, value.length() - 1);
        }
        return value;
    }

    public static boolean isValid(String normalized) {
        if (normalized == null || normalized.isEmpty()) {
            return false;
        }
        Uri uri = Uri.parse(normalized);
        String scheme = uri.getScheme();
        return ("http".equals(scheme) || "https".equals(scheme)) && uri.getHost() != null;
    }

    private static SharedPreferences prefs(Context context) {
        return context.getApplicationContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }
}
