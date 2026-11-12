package me.feseis.fish.logistics.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import me.feseis.fish.logistics.config.interceptor.AuthInterceptor;
import me.feseis.fish.logistics.service.StaffApplyService;
import me.feseis.fish.logistics.vo.staff.StaffApplyReqVO;
import me.feseis.fish.logistics.vo.staff.StaffApplyRespVO;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 员工接口：当前只有入职申请。需要登录，账号由 AuthInterceptor 注入。
 */
@RestController
@RequestMapping("/logistics/staff")
@RequiredArgsConstructor
public class StaffController {

    private final StaffApplyService staffApplyService;

    @PostMapping("/apply")
    public ResponseEntity<StaffApplyRespVO> apply(
            @RequestAttribute(AuthInterceptor.ATTR_ACCOUNT_ID) String accountId,
            @Valid @RequestBody StaffApplyReqVO req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(staffApplyService.apply(accountId, req));
    }
}
