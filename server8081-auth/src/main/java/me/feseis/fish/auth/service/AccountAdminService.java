package me.feseis.fish.auth.service;

import java.util.Optional;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import me.feseis.fish.auth.api.dto.AccountCreateReqDTO;
import me.feseis.fish.auth.api.dto.AccountLookupRespDTO;
import me.feseis.fish.auth.mapper.AccountMapper;
import me.feseis.fish.auth.mapper.model.AccountDetailRow;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 演示数据初始化：内部建账号，不校验验证码与密码复杂度。
 * 幂等：手机号已存在直接返回已有账号，不重复建。
 */
@Service
@RequiredArgsConstructor
public class AccountAdminService {

    private static final int DEFAULT_ROLE_ID = 1;
    private static final int STATUS_NORMAL = 1;

    private final AccountMapper accountMapper;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public AccountLookupRespDTO create(AccountCreateReqDTO req) {
        Optional<AccountDetailRow> existing = accountMapper.findDetailByPhone(req.phone());
        if (existing.isPresent()) {
            return toResponse(existing.get());
        }

        String accountId = UUID.randomUUID().toString();
        try {
            accountMapper.insertAccount(accountId, req.phone());
            accountMapper.insertProfileWithName(UUID.randomUUID().toString(), accountId, req.realName());
            accountMapper.insertPassword(UUID.randomUUID().toString(), accountId,
                    passwordEncoder.encode(req.password()));
            accountMapper.insertRole(accountId, DEFAULT_ROLE_ID);
        } catch (DuplicateKeyException e) {
            // 并发建同一手机号，回查已有账号
            return accountMapper.findDetailByPhone(req.phone())
                    .map(AccountAdminService::toResponse)
                    .orElseThrow(() -> new IllegalStateException("并发建账号失败", e));
        }
        return new AccountLookupRespDTO(accountId, req.phone(), STATUS_NORMAL, req.realName());
    }

    private static AccountLookupRespDTO toResponse(AccountDetailRow row) {
        return new AccountLookupRespDTO(row.getAccountId(), row.getPhone(),
                row.getStatus(), row.getRealName());
    }
}
