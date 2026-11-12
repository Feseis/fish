package me.feseis.fish.auth.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import me.feseis.fish.auth.api.AuthApiPaths;
import me.feseis.fish.auth.api.dto.AccountCreateReqDTO;
import me.feseis.fish.auth.api.dto.AccountLookupRespDTO;
import me.feseis.fish.auth.exp.AuthException;
import me.feseis.fish.auth.exp.ErrorCode;
import me.feseis.fish.auth.mapper.AccountMapper;
import me.feseis.fish.auth.mapper.model.AccountDetailRow;
import me.feseis.fish.auth.service.AccountAdminService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 账号内部接口。姓名与账号状态以本服务为唯一权威，业务侧只存快照。
 */
@RestController
@RequiredArgsConstructor
public class InternalAccountController {

    private final AccountMapper accountMapper;
    private final AccountAdminService accountAdminService;

    @GetMapping(AuthApiPaths.ACCOUNT_BY_PHONE)
    public AccountLookupRespDTO findByPhone(@RequestParam("phone") String phone) {
        AccountDetailRow row = accountMapper.findDetailByPhone(phone)
                .orElseThrow(() -> AuthException.of(ErrorCode.ACCOUNT_NOT_FOUND));
        return new AccountLookupRespDTO(row.getAccountId(), row.getPhone(),
                row.getStatus(), row.getRealName());
    }

    @PostMapping(AuthApiPaths.ACCOUNTS_INTERNAL)
    public AccountLookupRespDTO create(@Valid @RequestBody AccountCreateReqDTO req) {
        return accountAdminService.create(req);
    }
}
