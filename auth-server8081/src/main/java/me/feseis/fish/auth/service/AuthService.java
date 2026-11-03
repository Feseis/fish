package me.feseis.fish.auth.service;

import java.util.List;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.feseis.fish.auth.bo.login.LoginOutcome;
import me.feseis.fish.auth.dto.login.LoginOnPasswordRequest;
import me.feseis.fish.auth.dto.login.LoginOnPhoneOnlyRequest;
import me.feseis.fish.auth.exp.AuthException;
import me.feseis.fish.auth.exp.ErrorCode;
import me.feseis.fish.auth.mapper.AccountMapper;
import me.feseis.fish.auth.mapper.LoginLogMapper;
import me.feseis.fish.auth.mapper.model.AccountRow;
import me.feseis.fish.auth.mapper.model.PasswordRow;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * 登录编排。失败日志先落地再抛异常，因此本方法不得开启事务，否则日志会被回滚。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private static final int STATUS_NORMAL = 1;
    private static final int STATUS_FROZEN = 2;
    private static final int STATUS_CLOSED = 3;
    private static final int RESULT_FAILURE = 2;
    private static final String LOGIN_TYPE_PASSWORD = "PASSWORD";
    private static final String LOGIN_TYPE_SMS = "SMS";

    private final AccountMapper accountMapper;
    private final LoginLogMapper loginLogMapper;
    private final PasswordEncoder passwordEncoder;
    private final SessionService sessionService;
    private final SmsCodeService smsCodeService;

    public LoginOutcome login(LoginOnPasswordRequest req, String clientIp) {
        AccountRow account;
        PasswordRow password;
        List<Integer> roles;

        account = accountMapper.findByPhone(req.phone()).orElse(null);
        if (account == null) {
            fail(null, req.phone(), ErrorCode.NO_ACCOUNT, clientIp);
            throw AuthException.of(ErrorCode.NO_ACCOUNT);
        }
        requireActive(account, req.phone(), clientIp);

        password = accountMapper.findPassword(account.getAccountId()).orElse(null);
        if (password == null || !passwordEncoder.matches(req.password(), password.getPasswordHash())) {
            fail(account.getAccountId(), req.phone(), ErrorCode.PWD_WRONG, clientIp);
            throw AuthException.of(ErrorCode.PWD_WRONG);
        }

        roles = accountMapper.findRoleIds(account.getAccountId());
        return sessionService.issue(account.getAccountId(), req.phone(),
                password.getPwdVersion(), roles, LOGIN_TYPE_PASSWORD, clientIp);
    }

    /** 验证码登录：先核销验证码，再查账号与状态。 */
    public LoginOutcome loginBySms(LoginOnPhoneOnlyRequest req, String clientIp) {
        try {
            smsCodeService.verify(req.phone(), req.code());
        } catch (AuthException e) {
            fail(null, req.phone(), e.getErrorCode(), clientIp);
            throw e;
        }

        AccountRow account = accountMapper.findByPhone(req.phone()).orElse(null);
        if (account == null) {
            fail(null, req.phone(), ErrorCode.NO_ACCOUNT, clientIp);
            throw AuthException.of(ErrorCode.NO_ACCOUNT);
        }
        requireActive(account, req.phone(), clientIp);

        // 注册必定写入 auth_password，取不到视为数据不一致
        int pwdVersion = accountMapper.findPassword(account.getAccountId())
                .map(PasswordRow::getPwdVersion)
                .orElseThrow(() -> AuthException.of(ErrorCode.INTERNAL, "账号缺少密码记录"));
        List<Integer> roles = accountMapper.findRoleIds(account.getAccountId());
        return sessionService.issue(account.getAccountId(), req.phone(),
                pwdVersion, roles, LOGIN_TYPE_SMS, clientIp);
    }

    private void requireActive(AccountRow account, String phone, String clientIp) {
        int status = account.getStatus();
        if (status == STATUS_FROZEN) {
            fail(account.getAccountId(), phone, ErrorCode.FROZEN, clientIp);
            throw AuthException.of(ErrorCode.FROZEN);
        }
        if (status == STATUS_CLOSED) {
            fail(account.getAccountId(), phone, ErrorCode.CLOSED, clientIp);
            throw AuthException.of(ErrorCode.CLOSED);
        }
        if (status != STATUS_NORMAL) {
            log.warn("非法账号状态 accountId={} status={}", account.getAccountId(), status);
            fail(account.getAccountId(), phone, ErrorCode.FROZEN, clientIp);
            throw AuthException.of(ErrorCode.FROZEN);
        }
    }

    /** 失败日志独立提交：result=2，session_id 为空。 */
    private void fail(String accountId, String phone, ErrorCode code, String clientIp) {
        loginLogMapper.insert(accountId, phone, RESULT_FAILURE, code.name(), null, clientIp);
    }
}
