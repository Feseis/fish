package com.feseis.fish.auth.api;

import com.feseis.fish.auth.model.ApiCallback;
import com.feseis.fish.auth.model.HealthResult;
import com.feseis.fish.auth.model.LoginResult;
import com.feseis.fish.auth.model.RefreshResult;
import com.feseis.fish.auth.model.VerifyResult;

/**
 * 认证服务接口。方法签名与后端 AuthController 的 7 个接口一一对应，另加 /auth/health 用于连通性自检。
 */
public interface AuthApi {

    void getSmsCode(String phone, ApiCallback<Void> callback);

    void register(String phone, String code, String password, String confirmPassword,
                  ApiCallback<LoginResult> callback);

    void loginByPassword(String phone, String password, ApiCallback<LoginResult> callback);

    void loginBySms(String phone, String code, ApiCallback<LoginResult> callback);

    void refresh(ApiCallback<RefreshResult> callback);

    void logout(ApiCallback<Void> callback);

    void verify(ApiCallback<VerifyResult> callback);

    void checkHealth(ApiCallback<HealthResult> callback);
}
