package me.feseis.fish.auth.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import me.feseis.fish.auth.bo.login.LoginOutcome;
import me.feseis.fish.auth.dto.login.LoginOnPasswordRequest;
import me.feseis.fish.auth.dto.login.LoginOnPhoneOnlyRequest;
import me.feseis.fish.auth.dto.login.LoginResponse;
import me.feseis.fish.auth.dto.refresh.RefreshResponse;
import me.feseis.fish.auth.dto.register.RegisterRequest;
import me.feseis.fish.auth.dto.sms.SendSmsCodeRequest;
import me.feseis.fish.auth.dto.verify.VerifyResponse;
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
    public ResponseEntity<Void> getSmsCode(@Valid @RequestBody SendSmsCodeRequest req) {
        smsCodeService.sendCode(req.phone());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/register")
    public ResponseEntity<LoginResponse> register(@Valid @RequestBody RegisterRequest req,
                                                  HttpServletRequest request) {
        LoginOutcome outcome = registerService.register(req, request.getRemoteAddr());
        return ResponseEntity.status(HttpStatus.CREATED)
                .header(HttpHeaders.SET_COOKIE, cookie(outcome).toString())
                .body(outcome.response());
    }

    @PostMapping("/login/password")
    public ResponseEntity<LoginResponse> loginByPassword(@Valid @RequestBody LoginOnPasswordRequest req,
                                                         HttpServletRequest request) {
        return loginResponse(authService.login(req, request.getRemoteAddr()));
    }

    @PostMapping("/login/sms")
    public ResponseEntity<LoginResponse> loginBySms(@Valid @RequestBody LoginOnPhoneOnlyRequest req,
                                                    HttpServletRequest request) {
        return loginResponse(authService.loginBySms(req, request.getRemoteAddr()));
    }

    @PostMapping("/refresh")
    public RefreshResponse refresh(@CookieValue(name = "refresh_token", required = false) String refreshToken) {
        return sessionService.refresh(refreshToken);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@CookieValue(name = "refresh_token", required = false) String refreshToken,
                                       @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false)
                                       String authorization) {
        sessionService.logout(refreshToken, bearer(authorization));
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie("", Duration.ZERO).toString())
                .build();
    }

    @PostMapping("/verify")
    public VerifyResponse verify(@RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false)
                                 String authorization) {
        return sessionService.verify(bearer(authorization));
    }

    private static String bearer(String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            return null;
        }
        return authorization.substring(7);
    }

    private ResponseEntity<LoginResponse> loginResponse(LoginOutcome outcome) {
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
