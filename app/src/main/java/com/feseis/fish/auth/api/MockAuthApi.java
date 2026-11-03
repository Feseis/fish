package com.feseis.fish.auth.api;

import android.os.Handler;
import android.os.Looper;

import com.feseis.fish.auth.model.ApiCallback;
import com.feseis.fish.auth.model.ApiResult;
import com.feseis.fish.auth.model.AuthError;
import com.feseis.fish.auth.model.LoginResult;
import com.feseis.fish.auth.model.RefreshResult;
import com.feseis.fish.auth.model.VerifyResult;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * 纯前端阶段的假后端。固定账号、固定验证码，错误顺序与 AuthService 保持一致。
 */
public final class MockAuthApi implements AuthApi {

    public static final String MOCK_PHONE = "13800000000";
    public static final String MOCK_PASSWORD = "Fish@12345";
    public static final String MOCK_SMS_CODE = "123456";
    public static final String MOCK_EXPIRED_SMS_CODE = "000000";
    public static final String MOCK_FROZEN_PHONE = "13800000001";
    public static final String MOCK_CLOSED_PHONE = "13800000002";

    private static final long RESPONSE_DELAY_MS = 400L;
    private static final int ACCESS_TTL_HOURS = 2;
    private static final int MOCK_ROLE_ID = 1;
    private static final String MOCK_ACCOUNT_ID = "mock-account-0001";
    private static final String MOCK_SESSION_ID = "mock-session-0001";

    private final Handler handler = new Handler(Looper.getMainLooper());
    private LoginResult current;

    @Override
    public void getSmsCode(String phone, ApiCallback<Void> callback) {
        this.reply(ApiResult.<Void>success(null), callback);
    }

    @Override
    public void register(String phone, String code, String password, String confirmPassword,
                         ApiCallback<LoginResult> callback) {
        if (MOCK_EXPIRED_SMS_CODE.equals(code)) {
            this.reply(MockAuthApi.<LoginResult>failure(AuthError.SMS_EXPIRED, "验证码已过期"), callback);
            return;
        }
        if (!MOCK_SMS_CODE.equals(code)) {
            this.reply(MockAuthApi.<LoginResult>failure(AuthError.SMS_WRONG, "验证码错误"), callback);
            return;
        }
        if (this.isKnownPhone(phone)) {
            this.reply(MockAuthApi.<LoginResult>failure(AuthError.PHONE_EXISTS, "手机号已注册"), callback);
            return;
        }
        if (password == null || !password.equals(confirmPassword)) {
            this.reply(MockAuthApi.<LoginResult>failure(AuthError.INVALID_PARAM, "两次输入的密码不一致"), callback);
            return;
        }
        this.current = this.issue(phone);
        this.reply(ApiResult.success(this.current), callback);
    }

    @Override
    public void loginByPassword(String phone, String password, ApiCallback<LoginResult> callback) {
        if (!this.isKnownPhone(phone)) {
            this.reply(MockAuthApi.<LoginResult>failure(AuthError.NO_ACCOUNT, "账号不存在"), callback);
            return;
        }
        if (MOCK_FROZEN_PHONE.equals(phone)) {
            this.reply(MockAuthApi.<LoginResult>failure(AuthError.FROZEN, "账号已冻结"), callback);
            return;
        }
        if (MOCK_CLOSED_PHONE.equals(phone)) {
            this.reply(MockAuthApi.<LoginResult>failure(AuthError.CLOSED, "账号已注销"), callback);
            return;
        }
        if (!MOCK_PASSWORD.equals(password)) {
            this.reply(MockAuthApi.<LoginResult>failure(AuthError.PWD_WRONG, "手机号或密码错误"), callback);
            return;
        }
        this.current = this.issue(phone);
        this.reply(ApiResult.success(this.current), callback);
    }

    @Override
    public void loginBySms(String phone, String code, ApiCallback<LoginResult> callback) {
        // 后端先核销验证码，再查账号与状态。
        if (MOCK_EXPIRED_SMS_CODE.equals(code)) {
            this.reply(MockAuthApi.<LoginResult>failure(AuthError.SMS_EXPIRED, "验证码已过期"), callback);
            return;
        }
        if (!MOCK_SMS_CODE.equals(code)) {
            this.reply(MockAuthApi.<LoginResult>failure(AuthError.SMS_WRONG, "验证码错误"), callback);
            return;
        }
        if (!this.isKnownPhone(phone)) {
            this.reply(MockAuthApi.<LoginResult>failure(AuthError.NO_ACCOUNT, "账号不存在"), callback);
            return;
        }
        if (MOCK_FROZEN_PHONE.equals(phone)) {
            this.reply(MockAuthApi.<LoginResult>failure(AuthError.FROZEN, "账号已冻结"), callback);
            return;
        }
        if (MOCK_CLOSED_PHONE.equals(phone)) {
            this.reply(MockAuthApi.<LoginResult>failure(AuthError.CLOSED, "账号已注销"), callback);
            return;
        }
        this.current = this.issue(phone);
        this.reply(ApiResult.success(this.current), callback);
    }

    @Override
    public void refresh(ApiCallback<RefreshResult> callback) {
        if (this.current == null) {
            this.reply(MockAuthApi.<RefreshResult>failure(AuthError.TOKEN_EXPIRED, "登录已失效"), callback);
            return;
        }
        RefreshResult result = new RefreshResult(this.newAccessToken(), this.accessExpiry());
        this.current = new LoginResult(this.current.getAccountId(), this.current.getPhone(),
                this.current.getRoles(), result.getAccessToken(), result.getAccessExpiresAt());
        this.reply(ApiResult.success(result), callback);
    }

    @Override
    public void logout(ApiCallback<Void> callback) {
        this.current = null;
        this.reply(ApiResult.<Void>success(null), callback);
    }

    @Override
    public void verify(ApiCallback<VerifyResult> callback) {
        if (this.current == null) {
            this.reply(MockAuthApi.<VerifyResult>failure(AuthError.TOKEN_EXPIRED, "登录已失效"), callback);
            return;
        }
        this.reply(ApiResult.success(new VerifyResult(MOCK_SESSION_ID, this.current.getAccountId(),
                this.current.getRoles())), callback);
    }

    private boolean isKnownPhone(String phone) {
        return MOCK_PHONE.equals(phone)
                || MOCK_FROZEN_PHONE.equals(phone)
                || MOCK_CLOSED_PHONE.equals(phone);
    }

    private LoginResult issue(String phone) {
        return new LoginResult(MOCK_ACCOUNT_ID, phone, this.roles(), this.newAccessToken(), this.accessExpiry());
    }

    private List<Integer> roles() {
        return Arrays.asList(MOCK_ROLE_ID);
    }

    private String newAccessToken() {
        return "mock-access-" + System.currentTimeMillis();
    }

    private String accessExpiry() {
        long expiresAt = System.currentTimeMillis() + ACCESS_TTL_HOURS * 60L * 60L * 1000L;
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'+08:00'", Locale.CHINA);
        return formatter.format(new Date(expiresAt));
    }

    private <T> void reply(ApiResult<T> result, ApiCallback<T> callback) {
        if (callback == null) {
            return;
        }
        this.handler.postDelayed(() -> callback.onResult(result), RESPONSE_DELAY_MS);
    }

    private static <T> ApiResult<T> failure(String code, String message) {
        return ApiResult.failure(new AuthError(code, message));
    }
}
