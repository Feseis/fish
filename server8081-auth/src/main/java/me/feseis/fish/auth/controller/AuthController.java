package me.feseis.fish.auth.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import me.feseis.fish.auth.bo.login.LoginOutcome;
import me.feseis.fish.contract.AuthHeaders;
import me.feseis.fish.auth.vo.login.LoginOnPasswordReqVO;
import me.feseis.fish.auth.vo.login.LoginOnPhoneOnlyReqVO;
import me.feseis.fish.auth.vo.login.LoginRespVO;
import me.feseis.fish.auth.vo.refresh.RefreshRespVO;
import me.feseis.fish.auth.vo.register.RegisterReqVO;
import me.feseis.fish.auth.vo.sms.SendSmsCodeReqVO;
import me.feseis.fish.auth.vo.verify.VerifyRespVO;
import me.feseis.fish.auth.config.properties.AuthProperties;
import me.feseis.fish.auth.service.AuthService;
import me.feseis.fish.auth.service.RegisterService;
import me.feseis.fish.auth.service.SessionService;
import me.feseis.fish.auth.service.SmsCodeService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 认证对外接口。client_ip 取 remoteAddr；当前客户端直连、无网关，不信任 X-Forwarded-For。
 */
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final SmsCodeService smsCodeService;
    private final RegisterService registerService;
    private final AuthService authService;
    private final SessionService sessionService;
    private final AuthProperties props;

    @PostMapping("/get-sms-code")
    public ResponseEntity<Void> getSmsCode(@Valid @RequestBody SendSmsCodeReqVO req) {
        smsCodeService.sendCode(req.phone());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/register")
    public ResponseEntity<LoginRespVO> register(@Valid @RequestBody RegisterReqVO req,
                                                  HttpServletRequest request) {
        LoginOutcome outcome = registerService.register(req, request.getRemoteAddr());
        return ResponseEntity.status(HttpStatus.CREATED)
                .header(HttpHeaders.SET_COOKIE, cookie(outcome).toString())
                .body(outcome.response());
    }

    @PostMapping("/login/password")
    public ResponseEntity<LoginRespVO> loginByPassword(@Valid @RequestBody LoginOnPasswordReqVO req,
                                                         HttpServletRequest request) {
        return loginResponse(authService.login(req, request.getRemoteAddr()));
    }

    @PostMapping("/login/sms")
    public ResponseEntity<LoginRespVO> loginBySms(@Valid @RequestBody LoginOnPhoneOnlyReqVO req,
                                                    HttpServletRequest request) {
        return loginResponse(authService.loginBySms(req, request.getRemoteAddr()));
    }

    @PostMapping("/refresh")
    public RefreshRespVO refresh(@CookieValue(name = "refresh_token", required = false) String refreshToken) {
        return sessionService.refresh(refreshToken);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@CookieValue(name = "refresh_token", required = false) String refreshToken,
                                       @RequestHeader(value = AuthHeaders.AUTHORIZATION, required = false)
                                       String authorization) {
        sessionService.logout(refreshToken, bearer(authorization));
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie("", Duration.ZERO).toString())
                .build();
    }

    @PostMapping("/verify")
    public VerifyRespVO verify(@RequestHeader(value = AuthHeaders.AUTHORIZATION, required = false)
                                 String authorization) {
        return sessionService.verify(bearer(authorization));
    }

    private static String bearer(String authorization) {
        if (authorization == null || !authorization.startsWith(AuthHeaders.BEARER_PREFIX)) {
            return null;
        }
        return authorization.substring(AuthHeaders.BEARER_PREFIX.length());
    }

    private ResponseEntity<LoginRespVO> loginResponse(LoginOutcome outcome) {
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie(outcome).toString())
                .body(outcome.response());
    }

    private ResponseCookie cookie(LoginOutcome outcome) {
        return cookie(outcome.refreshToken(), outcome.refreshTtl());
    }

    private ResponseCookie cookie(String value, Duration maxAge) {
        return ResponseCookie.from("refresh_token", value)
                .httpOnly(true)
                .secure(props.cookie().secure())
                .sameSite("Lax")
                .path("/auth")
                .maxAge(maxAge)
                .build();
    }
}
