package me.feseis.fish.auth.service;

import lombok.RequiredArgsConstructor;
import me.feseis.fish.auth.exp.AuthException;
import me.feseis.fish.auth.exp.ErrorCode;
import me.feseis.fish.auth.mapper.AccountMapper;
import me.feseis.fish.auth.mapper.model.AccountRow;
import me.feseis.fish.auth.vo.password.ResetPasswordReqVO;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 验证码重置密码。验证码由 SmsCodeService 校验并核销，pwd_version 加一使旧 refresh 失效。
 */
@Service
@RequiredArgsConstructor
public class PasswordResetService {

    private static final int STATUS_NORMAL = 1;
    private static final int STATUS_FROZEN = 2;
    private static final int STATUS_CLOSED = 3;

    private final SmsCodeService smsCodeService;
    private final AccountMapper accountMapper;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public void reset(ResetPasswordReqVO req) {
        this.smsCodeService.verify(req.phone(), req.code());

        AccountRow account = this.accountMapper.findByPhone(req.phone())
                .orElseThrow(() -> AuthException.of(ErrorCode.NO_ACCOUNT));
        if (account.getStatus() == STATUS_FROZEN) {
            throw AuthException.of(ErrorCode.FROZEN);
        }
        if (account.getStatus() == STATUS_CLOSED) {
            throw AuthException.of(ErrorCode.CLOSED);
        }
        if (account.getStatus() != STATUS_NORMAL) {
            throw AuthException.of(ErrorCode.FROZEN);
        }

        int affected = this.accountMapper.updatePassword(account.getAccountId(),
                this.passwordEncoder.encode(req.newPassword()));
        if (affected == 0) {
            throw AuthException.of(ErrorCode.INTERNAL, "账号缺少密码记录");
        }
    }
}
