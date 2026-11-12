package me.feseis.fish.auth.controller;

import lombok.RequiredArgsConstructor;
import me.feseis.fish.auth.api.AuthApiPaths;
import me.feseis.fish.auth.api.dto.VerifyRespDTO;
import me.feseis.fish.auth.service.SessionService;
import me.feseis.fish.auth.vo.verify.VerifyRespVO;
import me.feseis.fish.contract.AuthHeaders;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

/**
 * 内部令牌校验。供其他服务调用，与对外 /auth/verify 分开，响应字段可以各自演进。
 */
@RestController
@RequiredArgsConstructor
public class InternalVerifyController {

    private final SessionService sessionService;

    @PostMapping(AuthApiPaths.VERIFY_INTERNAL)
    public VerifyRespDTO verify(
            @RequestHeader(value = AuthHeaders.AUTHORIZATION, required = false) String authorization) {
        VerifyRespVO view = sessionService.verify(bearer(authorization));
        return new VerifyRespDTO(view.sessionId(), view.accountId(), view.roles());
    }

    private static String bearer(String authorization) {
        if (authorization == null || !authorization.startsWith(AuthHeaders.BEARER_PREFIX)) {
            return null;
        }
        return authorization.substring(AuthHeaders.BEARER_PREFIX.length());
    }
}
