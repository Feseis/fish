package me.feseis.fish.auth.service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import me.feseis.fish.auth.bo.login.LoginOutcome;
import me.feseis.fish.auth.dto.login.LoginResponse;
import me.feseis.fish.auth.dto.refresh.RefreshResponse;
import me.feseis.fish.auth.dto.verify.VerifyResponse;
import me.feseis.fish.auth.config.properties.AuthProperties;
import me.feseis.fish.auth.exp.AuthException;
import me.feseis.fish.auth.exp.ErrorCode;
import me.feseis.fish.auth.mapper.AccountMapper;
import me.feseis.fish.auth.mapper.LoginLogMapper;
import me.feseis.fish.auth.mapper.SessionMapper;
import me.feseis.fish.auth.mapper.model.PasswordRow;
import me.feseis.fish.auth.mapper.model.SessionRow;
import me.feseis.fish.auth.service.TokenService.SessionView;
import org.springframework.stereotype.Service;

/**
 * 会话签发与生命周期。密码登录、验证码登录、注册后自动登录共用同一套签发逻辑，
 * loginType 由调用方区分；续期与登出在此收口。
 */
@Service
@RequiredArgsConstructor
public class SessionService {

    private static final int RESULT_SUCCESS = 1;
    private static final int ACCOUNT_NORMAL = 1;
    private static final int SESSION_ACTIVE = 1;
    private static final int SESSION_LOGGED_OUT = 2;
    private static final int SESSION_KICKED = 3;
    private static final int SESSION_EXPIRED = 4;
    private static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");

    private final SessionMapper sessionMapper;
    private final AccountMapper accountMapper;
    private final LoginLogMapper loginLogMapper;
    private final TokenService tokenService;
    private final AuthProperties props;

    public LoginOutcome issue(String accountId, String phone, int pwdVersion,
                              List<Integer> roles, String loginType, String clientIp) {
        String sessionId = UUID.randomUUID().toString();
        String refreshToken = tokenService.randomToken();
        String accessToken = tokenService.randomToken();
        Duration refreshTtl = props.token().refreshTtl();
        Duration accessTtl = props.token().accessTtl();
        LocalDateTime now = LocalDateTime.now();

        sessionMapper.insert(sessionId, accountId, tokenService.sha256Hex(refreshToken),
                pwdVersion, now, now.plus(refreshTtl));

        tokenService.storeAccess(accessToken, new SessionView(sessionId, accountId, pwdVersion), accessTtl);

        loginLogMapper.insert(accountId, phone, RESULT_SUCCESS, null, sessionId, clientIp);

        OffsetDateTime accessExpiresAt = now.plus(accessTtl).atZone(ZONE).toOffsetDateTime();
        LoginResponse body = new LoginResponse(accountId, mask(phone), roles, accessToken, accessExpiresAt);
        return new LoginOutcome(body, refreshToken, refreshTtl);
    }

    /** 续期：校验 refresh，签发新短期令牌，不轮换 refresh。 */
    public RefreshResponse refresh(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw AuthException.of(ErrorCode.TOKEN_EXPIRED);
        }
        SessionRow session = sessionMapper.findByRefreshHash(tokenService.sha256Hex(refreshToken))
                .orElseThrow(() -> AuthException.of(ErrorCode.TOKEN_EXPIRED));
        LocalDateTime now = LocalDateTime.now();
        if (session.getStatus() != SESSION_ACTIVE || !session.getExpireAt().isAfter(now)) {
            throw AuthException.of(ErrorCode.TOKEN_EXPIRED);
        }

        int accountStatus = accountMapper.findStatus(session.getAccountId())
                .orElseThrow(() -> AuthException.of(ErrorCode.TOKEN_EXPIRED));
        if (accountStatus == 2 || accountStatus == 3) {
            sessionMapper.close(session.getSessionId(), SESSION_KICKED, now);
            throw AuthException.of(accountStatus == 2 ? ErrorCode.FROZEN : ErrorCode.CLOSED);
        }
        if (accountStatus != ACCOUNT_NORMAL) {
            sessionMapper.close(session.getSessionId(), SESSION_KICKED, now);
            throw AuthException.of(ErrorCode.FROZEN);
        }

        int pwdVersion = accountMapper.findPassword(session.getAccountId())
                .map(PasswordRow::getPwdVersion)
                .orElseThrow(() -> AuthException.of(ErrorCode.INTERNAL, "账号缺少密码记录"));
        if (pwdVersion != session.getPwdVersion()) {
            sessionMapper.close(session.getSessionId(), SESSION_EXPIRED, now);
            throw AuthException.of(ErrorCode.TOKEN_EXPIRED);
        }

        Duration accessTtl = props.token().accessTtl();
        String accessToken = tokenService.randomToken();
        tokenService.storeAccess(accessToken,
                new SessionView(session.getSessionId(), session.getAccountId(), pwdVersion), accessTtl);
        sessionMapper.touch(session.getSessionId(), now);
        OffsetDateTime accessExpiresAt = now.plus(accessTtl).atZone(ZONE).toOffsetDateTime();
        return new RefreshResponse(accessToken, accessExpiresAt);
    }

    /** 校验短期令牌，供下游服务换取身份。只读 Redis 与角色表，不复核账号状态与密码版本。 */
    public VerifyResponse verify(String accessToken) {
        SessionView view = tokenService.validate(accessToken)
                .orElseThrow(() -> AuthException.of(ErrorCode.TOKEN_EXPIRED));
        List<Integer> roles = accountMapper.findRoleIds(view.accountId());
        return new VerifyResponse(view.sessionId(), view.accountId(), roles);
    }

    /** 登出：结束会话并吊销本次携带的短期令牌，重复调用无副作用。 */
    public void logout(String refreshToken, String accessToken) {
        LocalDateTime now = LocalDateTime.now();
        if (refreshToken != null && !refreshToken.isBlank()) {
            sessionMapper.findByRefreshHash(tokenService.sha256Hex(refreshToken))
                    .filter(s -> s.getStatus() == SESSION_ACTIVE)
                    .ifPresent(s -> sessionMapper.close(s.getSessionId(), SESSION_LOGGED_OUT, now));
        }
        tokenService.revoke(accessToken);
    }

    private static String mask(String phone) {
        if (phone == null || phone.length() != 11) {
            return phone;
        }
        return phone.substring(0, 3) + "****" + phone.substring(7);
    }
}
