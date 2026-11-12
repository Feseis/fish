package com.feseis.fish.auth.api;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import com.feseis.fish.auth.config.AuthEndpoint;
import com.feseis.fish.auth.model.LoginResult;
import com.feseis.fish.auth.store.PersistentSessionStore;
import com.feseis.fish.auth.store.SessionStore;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 令牌续期：用持久化的 refresh 令牌换新 access。
 * 多个请求同时 401 时只有第一个真正发起刷新，其余复用三秒内的成功结果。
 */
public final class TokenRefresher {

    private static final Object LOCK = new Object();
    private static final int CONNECT_TIMEOUT_MS = 5000;
    private static final int READ_TIMEOUT_MS = 8000;
    private static final long REUSE_WINDOW_MS = 3000L;
    private static final String REFRESH_PATH = "/auth/refresh";

    private static Context appContext;
    private static ExecutorService executor;
    private static Handler mainHandler;
    private static long lastSuccessAt;

    private TokenRefresher() {
    }

    public static void init(Context context) {
        appContext = context.getApplicationContext();
        executor = Executors.newSingleThreadExecutor();
        mainHandler = new Handler(Looper.getMainLooper());
    }

    /**
     * 后台线程调用。refresh 失效或服务地址缺失时清空会话；网络异常保留会话以便下次重试。
     */
    public static boolean refreshNow() {
        synchronized (LOCK) {
            if (System.currentTimeMillis() - lastSuccessAt < REUSE_WINDOW_MS) {
                return true;
            }
            String refreshToken = PersistentSessionStore.getRefreshToken();
            String base = AuthEndpoint.get(appContext);
            if (refreshToken.isEmpty() || base.isEmpty()) {
                clearSession();
                return false;
            }

            HttpURLConnection connection = null;
            try {
                connection = (HttpURLConnection) new URL(base + REFRESH_PATH).openConnection();
                connection.setRequestMethod("POST");
                connection.setConnectTimeout(CONNECT_TIMEOUT_MS);
                connection.setReadTimeout(READ_TIMEOUT_MS);
                connection.setRequestProperty("Accept", "application/json");
                connection.setRequestProperty("Cookie", "refresh_token=" + refreshToken);

                int status = connection.getResponseCode();
                String text = readAll(status >= 400
                        ? connection.getErrorStream() : connection.getInputStream());
                if (status < 200 || status >= 300) {
                    if (status >= 400 && status < 500) {
                        clearSession();
                    }
                    return false;
                }

                JSONObject json = new JSONObject(text);
                String accessToken = json.optString("accessToken", "");
                if (accessToken.isEmpty()) {
                    clearSession();
                    return false;
                }
                LoginResult current = SessionStore.get().getSession();
                String accountId = current == null
                        ? PersistentSessionStore.getAccountId() : current.getAccountId();
                String phone = current == null
                        ? PersistentSessionStore.getPhone() : current.getPhone();
                List<Integer> roles = current == null
                        ? PersistentSessionStore.getRoles() : current.getRoles();
                SessionStore.get().save(new LoginResult(accountId, phone, roles,
                        accessToken, json.optString("accessExpiresAt", "")));
                lastSuccessAt = System.currentTimeMillis();
                return true;
            } catch (IOException e) {
                return false;
            } catch (JSONException e) {
                clearSession();
                return false;
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }
        }
    }

    /** 异步版本，回调在主线程。 */
    public static void refreshAsync(Callback callback) {
        executor.execute(() -> {
            boolean success = refreshNow();
            mainHandler.post(() -> callback.onFinished(success));
        });
    }

    private static void clearSession() {
        SessionStore.get().clear();
        PersistentSessionStore.clear();
        lastSuccessAt = 0;
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

    public interface Callback {
        void onFinished(boolean success);
    }
}
