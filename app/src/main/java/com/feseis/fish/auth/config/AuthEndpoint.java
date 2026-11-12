package com.feseis.fish.auth.config;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;

import java.util.regex.Pattern;

/**
 * 认证服务地址。开发调试阶段由用户在首页手动填写 IP，端口固定为 8081。
 * 地址变化对每次请求即时生效。
 */
public final class AuthEndpoint {

    private static final String PREFS_NAME = "fish_prefs";
    private static final String KEY_BASE_URL = "auth_base_url";
    private static final String DEFAULT_PORT = "8081";
    private static final Pattern HOST_PATTERN = Pattern.compile("^[0-9a-zA-Z.-]+$");

    private AuthEndpoint() {
    }

    /** 完整 baseUrl，供请求拼装使用。 */
    public static String get(Context context) {
        return normalize(prefs(context).getString(KEY_BASE_URL, ""));
    }

    /** 只返回主机部分，供输入框回填。 */
    public static String host(Context context) {
        String base = get(context);
        if (base.isEmpty()) {
            return "";
        }
        String host = Uri.parse(base).getHost();
        return host == null ? "" : host;
    }

    public static void save(Context context, String raw) {
        prefs(context).edit().putString(KEY_BASE_URL, normalize(raw)).apply();
    }

    /**
     * 归一化为 http://&lt;host&gt;:8081。输入可带 scheme 与端口，端口一律以默认值覆盖。
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
        String host = Uri.parse(value).getHost();
        if (host == null || !HOST_PATTERN.matcher(host).matches()) {
            return "";
        }
        return "http://" + host + ":" + DEFAULT_PORT;
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
