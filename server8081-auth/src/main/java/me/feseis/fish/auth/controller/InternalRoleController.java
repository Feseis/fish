package me.feseis.fish.auth.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import me.feseis.fish.auth.api.AuthApiPaths;
import me.feseis.fish.auth.api.dto.RoleGrantReqDTO;
import me.feseis.fish.auth.api.dto.RoleGrantRespDTO;
import me.feseis.fish.auth.exp.AuthException;
import me.feseis.fish.auth.exp.ErrorCode;
import me.feseis.fish.auth.mapper.AccountMapper;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * 角色内部接口。授予幂等，已存在返回 200 且 granted=false。
 */
@RestController
@RequiredArgsConstructor
public class InternalRoleController {

    private final AccountMapper accountMapper;

    @PostMapping(AuthApiPaths.GRANT_ROLE_INTERNAL)
    public RoleGrantRespDTO grant(@Valid @RequestBody RoleGrantReqDTO req) {
        if (accountMapper.findStatus(req.accountId()).isEmpty()) {
            throw AuthException.of(ErrorCode.ACCOUNT_NOT_FOUND);
        }
        int affected = accountMapper.grantRoleIfAbsent(req.accountId(), req.roleId());
        return new RoleGrantRespDTO(req.accountId(), req.roleId(), affected > 0);
    }
}
