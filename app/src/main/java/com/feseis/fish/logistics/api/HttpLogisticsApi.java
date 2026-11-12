package com.feseis.fish.logistics.api;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import com.feseis.fish.auth.config.AuthEndpoint;
import com.feseis.fish.auth.model.ApiCallback;
import com.feseis.fish.auth.model.ApiResult;
import com.feseis.fish.auth.model.AuthError;
import com.feseis.fish.auth.model.LoginResult;
import com.feseis.fish.auth.store.SessionStore;
import com.feseis.fish.logistics.model.AddressDraft;
import com.feseis.fish.logistics.model.Poi;
import com.feseis.fish.logistics.model.RegeoResult;
import com.feseis.fish.logistics.model.StaffApplyResult;
import com.feseis.fish.logistics.model.UserAddress;
import me.feseis.fish.contract.AuthHeaders;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 物流服务真实实现。基址取认证服务主机 + 8082，Bearer 取自会话。
 */
public final class HttpLogisticsApi implements LogisticsApi {

    private static final String LOGISTICS_PORT = "8082";
    private static final int CONNECT_TIMEOUT_MS = 5000;
    private static final int READ_TIMEOUT_MS = 8000;

    private final Context context;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public HttpLogisticsApi(Context context) {
        this.context = context;
    }

    @Override
    public void applyStaff(String voucherCode, String applyRemark,
                           ApiCallback<StaffApplyResult> callback) {
        JSONObject body = new JSONObject();
        put(body, "voucherCode", voucherCode);
        put(body, "applyRemark", applyRemark);
        send("POST", "/logistics/staff/apply", body,
                text -> toStaffApply(new JSONObject(text)), callback);
    }

    @Override
    public void listAddresses(ApiCallback<List<UserAddress>> callback) {
        send("GET", "/logistics/address/user", null, text -> {
            JSONArray array = new JSONArray(text);
            List<UserAddress> addresses = new ArrayList<>();
            for (int i = 0; i < array.length(); i++) {
                addresses.add(toAddress(array.getJSONObject(i)));
            }
            return addresses;
        }, callback);
    }

    @Override
    public void createAddress(AddressDraft draft, ApiCallback<UserAddress> callback) {
        send("POST", "/logistics/address/user", toBody(draft),
                text -> toAddress(new JSONObject(text)), callback);
    }

    @Override
    public void updateAddress(String userAddressId, AddressDraft draft,
                              ApiCallback<UserAddress> callback) {
        send("PUT", "/logistics/address/user/" + userAddressId, toBody(draft),
                text -> toAddress(new JSONObject(text)), callback);
    }

    @Override
    public void deleteAddress(String userAddressId, ApiCallback<Void> callback) {
        send("DELETE", "/logistics/address/user/" + userAddressId, null,
                text -> null, callback);
    }

    @Override
    public void regeo(double lng, double lat, ApiCallback<RegeoResult> callback) {
        String path = "/logistics/address/regeo?lng=" + lng + "&lat=" + lat;
        send("GET", path, null, text -> {
            JSONObject json = new JSONObject(text);
            JSONArray array = json.optJSONArray("pois");
            List<Poi> pois = new ArrayList<>();
            if (array != null) {
                for (int i = 0; i < array.length(); i++) {
                    pois.add(toPoi(array.getJSONObject(i)));
                }
            }
            return new RegeoResult(
                    json.optString("formattedAddress", ""),
                    json.optString("province", ""),
                    json.optString("city", ""),
                    json.optString("district", ""),
                    json.optString("adcode", ""),
                    pois);
        }, callback);
    }

    @Override
    public void searchAddressPois(String keyword, String city, ApiCallback<List<Poi>> callback) {
        String path = "/logistics/address/search?keyword=" + encode(keyword)
                + (city == null || city.isEmpty() ? "" : "&city=" + encode(city));
        send("GET", path, null, text -> {
            JSONArray array = new JSONArray(text);
            List<Poi> pois = new ArrayList<>();
            for (int i = 0; i < array.length(); i++) {
                pois.add(toPoi(array.getJSONObject(i)));
            }
            return pois;
        }, callback);
    }

    /** 统一请求：GET/DELETE 无请求体，POST/PUT 传 JSON 对象。 */
    private <T> void send(String method, String path, JSONObject body,
                          BodyParser<T> parser, ApiCallback<T> callback) {
        String host = AuthEndpoint.host(this.context);
        if (host.isEmpty()) {
            this.mainHandler.post(() -> callback.onResult(ApiResult.<T>failure(
                    new AuthError(AuthError.ENDPOINT_MISSING, "请先在首页填写服务地址"))));
            return;
        }
        String base = "http://" + host + ":" + LOGISTICS_PORT;

        this.executor.execute(() -> {
            HttpURLConnection connection = null;
            try {
                connection = (HttpURLConnection) new URL(base + path).openConnection();
                connection.setRequestMethod(method);
                connection.setConnectTimeout(CONNECT_TIMEOUT_MS);
                connection.setReadTimeout(READ_TIMEOUT_MS);
                connection.setRequestProperty("Accept", "application/json");

                LoginResult session = SessionStore.get().getSession();
                if (session != null && session.getAccessToken() != null) {
                    connection.setRequestProperty(AuthHeaders.AUTHORIZATION,
                            AuthHeaders.BEARER_PREFIX + session.getAccessToken());
                }

                if (body != null) {
                    connection.setDoOutput(true);
                    connection.setRequestProperty("Content-Type", "application/json; charset=utf-8");
                    try (OutputStream out = connection.getOutputStream()) {
                        out.write(body.toString().getBytes(StandardCharsets.UTF_8));
                    }
                }

                int status = connection.getResponseCode();
                String text = readAll(status >= 400
                        ? connection.getErrorStream() : connection.getInputStream());

                if (status >= 200 && status < 300) {
                    this.deliver(ApiResult.success(parser.parse(text)), callback);
                } else {
                    this.deliver(ApiResult.<T>failure(toError(status, text)), callback);
                }
            } catch (JSONException e) {
                this.deliver(ApiResult.<T>failure(
                        new AuthError(AuthError.INTERNAL, "响应解析失败")), callback);
            } catch (Exception e) {
                String reason = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
                this.deliver(ApiResult.<T>failure(
                        new AuthError(AuthError.NETWORK, reason)), callback);
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }
        });
    }

    private <T> void deliver(ApiResult<T> result, ApiCallback<T> callback) {
        this.mainHandler.post(() -> callback.onResult(result));
    }

    private static StaffApplyResult toStaffApply(JSONObject json) {
        return new StaffApplyResult(
                json.optString("staffId", ""),
                json.optString("staffNo", ""),
                json.optString("staffName", ""),
                json.optInt("staffRole", 0),
                json.optString("siteCode", ""),
                json.optString("siteName", ""),
                json.optString("appliedAt", ""));
    }

    private static UserAddress toAddress(JSONObject json) {
        return new UserAddress(
                json.optString("userAddressId", ""),
                json.optString("poiId", ""),
                json.optString("poiName", ""),
                json.optString("adcode", ""),
                json.optString("province", ""),
                json.optString("city", ""),
                json.optString("district", ""),
                json.optDouble("lng", 0),
                json.optDouble("lat", 0),
                json.optString("contactName", ""),
                json.optString("contactPhone", ""),
                json.optString("detailAddr", ""),
                json.optString("tag", ""),
                json.optString("createdAt", ""));
    }

    private static Poi toPoi(JSONObject json) {
        return new Poi(
                json.optString("poiId", ""),
                json.optString("poiName", ""),
                json.optString("adcode", ""),
                json.optString("province", ""),
                json.optString("city", ""),
                json.optString("district", ""),
                json.optString("address", ""),
                json.optDouble("lng", 0),
                json.optDouble("lat", 0));
    }

    private static String encode(String value) {
        try {
            return URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8.name());
        } catch (UnsupportedEncodingException e) {
            return "";
        }
    }

    private static JSONObject toBody(AddressDraft draft) {
        JSONObject body = new JSONObject();
        put(body, "poiId", draft.getPoiId());
        put(body, "poiName", draft.getPoiName());
        put(body, "adcode", draft.getAdcode());
        put(body, "province", draft.getProvince());
        put(body, "city", draft.getCity());
        put(body, "district", draft.getDistrict());
        put(body, "contactName", draft.getContactName());
        put(body, "contactPhone", draft.getContactPhone());
        put(body, "detailAddr", draft.getDetailAddr());
        put(body, "tag", draft.getTag());
        try {
            body.put("lng", draft.getLng());
            body.put("lat", draft.getLat());
        } catch (JSONException ignored) {
            // 常量键，不会触发
        }
        return body;
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

    private interface BodyParser<T> {
        T parse(String text) throws JSONException;
    }
}
