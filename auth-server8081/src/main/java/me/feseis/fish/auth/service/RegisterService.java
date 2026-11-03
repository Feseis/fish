package me.feseis.fish.auth.service;

import java.util.List;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import me.feseis.fish.auth.bo.login.LoginOutcome;
import me.feseis.fish.auth.dto.register.RegisterRequest;
import me.feseis.fish.auth.exp.AuthException;
import me.feseis.fish.auth.exp.ErrorCode;
import me.feseis.fish.auth.mapper.AccountMapper;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 注册编排。三条 INSERT 必须原子，故开启事务；与 AuthService.login 的"不开事务"相反，
 * 那边是为了让失败日志独立提交。
 */
@Service
@RequiredArgsConstructor
public class RegisterService {

    /** V2 种子数据中的默认角色：1 收寄用户。 */
    private static final int DEFAULT_ROLE_ID = 1;
    private static final String LOGIN_TYPE_SMS = "SMS";

    private final SmsCodeService smsCodeService;
    private final AccountMapper accountMapper;
    private final SessionService sessionService;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public LoginOutcome register(RegisterRequest req, String clientIp) {
        // 先验码再查重：避免未持码者用本接口探测手机号是否已注册
        smsCodeService.verify(req.phone(), req.code());

        if (accountMapper.findByPhone(req.phone()).isPresent()) {
            throw AuthException.of(ErrorCode.PHONE_EXISTS);
        }

        String accountId = UUID.randomUUID().toString();
        try {
            accountMapper.insertAccount(accountId, req.phone());
            accountMapper.insertPassword(accountId, passwordEncoder.encode(req.password()));
            accountMapper.insertRole(accountId, DEFAULT_ROLE_ID);
        } catch (DuplicateKeyException e) {
            // 并发注册由 uk_phone 唯一索引兜底
            throw AuthException.of(ErrorCode.PHONE_EXISTS);
        }

        return sessionService.issue(accountId, req.phone(), 0,
                List.of(DEFAULT_ROLE_ID), LOGIN_TYPE_SMS, clientIp);
    }
}
