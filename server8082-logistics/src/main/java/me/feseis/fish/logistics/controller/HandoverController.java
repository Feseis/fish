package me.feseis.fish.logistics.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import me.feseis.fish.logistics.config.interceptor.AuthInterceptor;
import me.feseis.fish.logistics.vo.handover.HandoverCreateReqVO;
import me.feseis.fish.logistics.vo.handover.HandoverLoadReqVO;
import me.feseis.fish.logistics.vo.handover.HandoverRespVO;
import me.feseis.fish.logistics.service.HandoverService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 交接单（班次）：创建待发、装车、发车、到达、查询。全部要求在职员工。
 */
@RestController
@RequestMapping("/logistics/handover")
@RequiredArgsConstructor
public class HandoverController {

    private final HandoverService handoverService;

    @PostMapping
    public ResponseEntity<HandoverRespVO> create(
            @RequestAttribute(AuthInterceptor.ATTR_ACCOUNT_ID) String accountId,
            @Valid @RequestBody HandoverCreateReqVO req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(handoverService.create(accountId, req));
    }

    @PostMapping("/{handoverId}/load")
    public HandoverRespVO load(
            @RequestAttribute(AuthInterceptor.ATTR_ACCOUNT_ID) String accountId,
            @PathVariable String handoverId,
            @Valid @RequestBody HandoverLoadReqVO req) {
        return handoverService.load(accountId, handoverId, req);
    }

    @PostMapping("/{handoverId}/depart")
    public HandoverRespVO depart(
            @RequestAttribute(AuthInterceptor.ATTR_ACCOUNT_ID) String accountId,
            @PathVariable String handoverId) {
        return handoverService.depart(accountId, handoverId);
    }

    @PostMapping("/{handoverId}/arrive")
    public HandoverRespVO arrive(
            @RequestAttribute(AuthInterceptor.ATTR_ACCOUNT_ID) String accountId,
            @PathVariable String handoverId) {
        return handoverService.arrive(accountId, handoverId);
    }

    @GetMapping("/{handoverId}")
    public HandoverRespVO detail(
            @RequestAttribute(AuthInterceptor.ATTR_ACCOUNT_ID) String accountId,
            @PathVariable String handoverId) {
        return handoverService.detail(handoverId);
    }
}
