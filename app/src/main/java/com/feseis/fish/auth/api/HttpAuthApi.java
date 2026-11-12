package com.feseis.fish.auth.api;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import com.feseis.fish.auth.config.AuthEndpoint;
import com.feseis.fish.core.api.ApiCallback;
import com.feseis.fish.core.api.ApiResult;
import com.feseis.fish.core.model.AuthError;
import com.feseis.fish.auth.model.HealthResult;
import com.feseis.fish.auth.model.LoginResult;
import com.feseis.fish.auth.model.RefreshResult;
import com.feseis.fish.auth.model.VerifyResult;
import com.feseis.fish.auth.store.PersistentSessionStore;
import com.feseis.fish.auth.store.SessionStore;
import me.feseis.fish.contract.AuthHeaders;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 真实后端实现：HttpURLConnection + org.json，refresh 令牌由 CookieManager 托管。
 * 基址每次请求实时读取 {@link AuthEndpoint}，地址改完立即生效。
 */
public final class HttpAuthApi implements AuthApi {

    private static final int CONNECT_TIMEOUT_MS = 5000;
    private static final int READ_TIMEOUT_MS = 8000;

    static {
        CookieManager cookieManager = new CookieManager();
        cookieManager.setCookiePolicy(CookiePolicy.ACCEPT_ALL);
        CookieHandler.setDefault(cookieManager);
    }

    private final Context context;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public HttpAuthApi(Context context) {
        this.context = context.getApplicationContext();
    }

    @Override
    public void getSmsCode(String phone, ApiCallback<Void> callback) {
        JSONObject body = new JSONObject();
        put(body, "phone", phone);
        this.call("POST", "/auth/get-sms-code", body, false, false,
                json -> (Void) null, callback);
    }

    @Override
    public void register(String phone, String code, String password, String confirmPassword,
                         ApiCallback<LoginResult> callback) {
        JSONObject body = new JSONObject();
        put(body, "phone", phone);
        put(body, "code", code);
        put(body, "password", password);
        put(body, "confirmPassword", confirmPassword);
        this.call("POST", "/auth/register", body, false, false,
                HttpAuthApi::toLoginResult, callback);
    }

    @Override
    public void loginByPassword(String phone, String password, ApiCallback<LoginResult> callback) {
        JSONObject body = new JSONObject();
        put(body, "phone", phone);
        put(body, "password", password);
        this.call("POST", "/auth/login/password", body, false, false,
                HttpAuthApi::toLoginResult, callback);
    }

    @Override
    public void loginBySms(String phone, String code, ApiCallback<LoginResult> callback) {
        JSONObject body = new JSONObject();
        put(body, "phone", phone);
        put(body, "code", code);
        this.call("POST", "/auth/login/sms", body, false, false,
                HttpAuthApi::toLoginResult, callback);
    }

    @Override
    public void refresh(ApiCallback<RefreshResult> callback) {
        this.call("POST", "/auth/refresh", null, true, false, json -> {
            RefreshResult result = new RefreshResult(json.optString("accessToken", ""),
                    json.optString("accessExpiresAt", ""));
            LoginResult current = SessionStore.get().getSession();
            if (current != null) {
                SessionStore.get().save(new LoginResult(current.getAccountId(), current.getPhone(),
                        current.getRoles(), result.getAccessToken(), result.getAccessExpiresAt()));
            }
            return result;
        }, callback);
    }

    @Override
    public void logout(ApiCallback<Void> callback) {
        this.call("POST", "/auth/logout", null, true, false, json -> (Void) null,
                result -> {
                    if (result.isSuccess()) {
                        SessionStore.get().clear();
                        PersistentSessionStore.clear();
                    }
                    callback.onResult(result);
                });
    }

    @Override
    public void resetPassword(String phone, String code, String newPassword,
                              ApiCallback<Void> callback) {
        JSONObject body = new JSONObject();
        put(body, "phone", phone);
        put(body, "code", code);
        put(body, "newPassword", newPassword);
        this.call("POST", "/auth/password/reset", body, false, false,
                json -> (Void) null, callback);
    }

    @Override
    public void verify(ApiCallback<VerifyResult> callback) {
        this.call("POST", "/auth/verify", null, true, false, json -> new VerifyResult(
                json.optString("sessionId", ""),
                json.optString("accountId", ""),
                roles(json)), callback);
    }

    @Override
    public void checkHealth(ApiCallback<HealthResult> callback) {
        // 依赖未就绪时后端返回 503 但响应体仍是 HealthStatus，需要照常解析。
        this.call("GET", "/auth/health", null, false, true, json -> new HealthResult(
                json.optBoolean("database", false),
                json.optBoolean("redis", false)), callback);
    }

    private <T> void call(String method, String path, JSONObject payload, boolean authorized,
                          boolean parseErrorBody, Parser<T> parser, ApiCallback<T> callback) {
        String base = AuthEndpoint.get(this.context);
        if (base.isEmpty()) {
            this.mainHandler.post(() -> callback.onResult(ApiResult.failure(
                    new AuthError(AuthError.ENDPOINT_MISSING, "请先在首页填写认证服务地址"))));
            return;
        }
        this.executor.execute(() -> {
            try {
                Response response = this.execute(method, base + path, payload, authorized);
                if (response.status == 401 && authorized && !"/auth/refresh".equals(path)
                        && TokenRefresher.refreshNow()) {
                    response = this.execute(method, base + path, payload, authorized);
                }
                if (response.status >= 200 && response.status < 300) {
                    T parsed = parser.parse(parseJson(response.text));
                    rememberRefreshToken(response.headers, parsed);
                    this.deliver(ApiResult.success(parsed), callback);
                } else if (parseErrorBody) {
                    this.deliver(ApiResult.success(parser.parse(parseJson(response.text))), callback);
                } else {
                    this.deliver(ApiResult.failure(toError(response.status, response.text)), callback);
                }
            } catch (JSONException e) {
                this.deliver(ApiResult.<T>failure(
                        new AuthError(AuthError.INTERNAL, "响应解析失败")), callback);
            } catch (Exception e) {
                String reason = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
                this.deliver(ApiResult.<T>failure(new AuthError(AuthError.NETWORK, reason)), callback);
            }
        });
    }

    /** 执行单次请求，连接在返回前关闭。 */
    private Response execute(String method, String url, JSONObject payload,
                             boolean authorized) throws IOException {
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) new URL(url).openConnection();
            connection.setRequestMethod(method);
            connection.setConnectTimeout(CONNECT_TIMEOUT_MS);
            connection.setReadTimeout(READ_TIMEOUT_MS);
            connection.setRequestProperty("Accept", "application/json");
            if (payload != null) {
                connection.setDoOutput(true);
                connection.setRequestProperty("Content-Type", "application/json; charset=utf-8");
                try (OutputStream out = connection.getOutputStream()) {
                    out.write(payload.toString().getBytes(StandardCharsets.UTF_8));
                }
            }
            if (authorized) {
                LoginResult session = SessionStore.get().getSession();
                if (session != null && session.getAccessToken() != null) {
                    connection.setRequestProperty(AuthHeaders.AUTHORIZATION,
                            AuthHeaders.BEARER_PREFIX + session.getAccessToken());
                }
            }
            int status = connection.getResponseCode();
            String text = readAll(status >= 400
                    ? connection.getErrorStream() : connection.getInputStream());
            return new Response(status, text, connection.getHeaderFields());
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    /** 登录与注册成功时把 Set-Cookie 里的 refresh 令牌落盘，供冷启动恢复。 */
    private static void rememberRefreshToken(Map<String, List<String>> headers, Object parsed) {
        if (!(parsed instanceof LoginResult)) {
            return;
        }
        String token = extractRefreshToken(headers);
        if (token == null || token.isEmpty()) {
            return;
        }
        LoginResult login = (LoginResult) parsed;
        PersistentSessionStore.save(login.getAccountId(), login.getPhone(),
                login.getRoles(), token);
    }

    private static String extractRefreshToken(Map<String, List<String>> headers) {
        if (headers == null) {
            return null;
        }
        for (Map.Entry<String, List<String>> entry : headers.entrySet()) {
            if (entry.getKey() == null || !"Set-Cookie".equalsIgnoreCase(entry.getKey())) {
                continue;
            }
            for (String value : entry.getValue()) {
                if (value == null || !value.startsWith("refresh_token=")) {
                    continue;
                }
                int end = value.indexOf(';');
                return end < 0
                        ? value.substring("refresh_token=".length())
                        : value.substring("refresh_token=".length(), end);
            }
        }
        return null;
    }

    private static final class Response {
        private final int status;
        private final String text;
        private final Map<String, List<String>> headers;

        private Response(int status, String text, Map<String, List<String>> headers) {
            this.status = status;
            this.text = text;
            this.headers = headers;
        }
    }

    private <T> void deliver(ApiResult<T> result, ApiCallback<T> callback) {
        this.mainHandler.post(() -> callback.onResult(result));
    }

    private static LoginResult toLoginResult(JSONObject json) {
        return new LoginResult(json.optString("accountId", ""),
                json.optString("phone", ""),
                roles(json),
                json.optString("accessToken", ""),
                json.optString("accessExpiresAt", ""));
    }

    private static List<Integer> roles(JSONObject json) {
        List<Integer> roles = new ArrayList<>();
        JSONArray array = json.optJSONArray("roles");
        if (array != null) {
            for (int i = 0; i < array.length(); i++) {
                roles.add(array.optInt(i));
            }
        }
        return roles;
    }

    private static JSONObject parseJson(String text) throws JSONException {
        return text == null || text.isEmpty() ? new JSONObject() : new JSONObject(text);
    }

    private static AuthError toError(int status, String text) {
        String code = "";
        String detail = "";
        try {
            JSONObject json = new JSONObject(text);
            code = json.optString("code", "");
            detail = json.optString("detail", "");
        } catch (JSONException ignored) {
            // 非 problem+json，走下面的兜底
        }
        if (code.isEmpty()) {
            if (status == 404) {
                code = AuthError.NOT_FOUND;
            } else if (status >= 500) {
                code = AuthError.INTERNAL;
            } else {
                code = AuthError.INVALID_PARAM;
            }
        }
        if (detail.isEmpty()) {
            detail = "请求失败（HTTP " + status + "）";
        }
        return new AuthError(code, detail);
    }

    private static String readAll(InputStream stream) throws IOException {
        if (stream == null) {
            return "";
        }
        try (InputStream in = stream; ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[4096];
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
            return out.toString(StandardCharsets.UTF_8.name());
        }
    }

    private static void put(JSONObject body, String key, String value) {
        try {
            body.put(key, value);
        } catch (JSONException ignored) {
            // key 为常量，不会触发
        }
    }

    private interface Parser<T> {
        T parse(JSONObject json) throws JSONException;
    }
}
