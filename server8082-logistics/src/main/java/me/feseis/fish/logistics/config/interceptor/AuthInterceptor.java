package me.feseis.fish.logistics.config.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import me.feseis.fish.contract.AuthHeaders;
import me.feseis.fish.logistics.client.AuthClient;
import me.feseis.fish.logistics.exp.BizException;
import me.feseis.fish.logistics.exp.ErrorCode;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 用户态接口的令牌校验：转发 Bearer 到 auth，换取 accountId 后放入请求属性。
 */
@Component
@RequiredArgsConstructor
public class AuthInterceptor implements HandlerInterceptor {

    public static final String ATTR_ACCOUNT_ID = "authAccountId";

    private final AuthClient authClient;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String authorization = request.getHeader(AuthHeaders.AUTHORIZATION);
        if (authorization == null || !authorization.startsWith(AuthHeaders.BEARER_PREFIX)) {
            throw BizException.of(ErrorCode.UNAUTHORIZED);
        }
        String accountId = authClient.verify(authorization.substring(AuthHeaders.BEARER_PREFIX.length()));
        request.setAttribute(ATTR_ACCOUNT_ID, accountId);
        return true;
    }
}
