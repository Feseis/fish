package me.feseis.fish.logistics.controller;

import java.util.List;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import me.feseis.fish.logistics.vo.waybill.WaybillCreateReqVO;
import me.feseis.fish.logistics.vo.waybill.WaybillDetailRespVO;
import me.feseis.fish.logistics.vo.waybill.WaybillCreateRespVO;
import me.feseis.fish.logistics.vo.waybill.WaybillCancelReqVO;
import me.feseis.fish.logistics.vo.waybill.WaybillDeliverReqVO;
import me.feseis.fish.logistics.vo.waybill.WaybillExceptionReqVO;
import me.feseis.fish.logistics.vo.waybill.WaybillTraceRespVO;
import me.feseis.fish.logistics.vo.waybill.WaybillOperationRespVO;
import me.feseis.fish.logistics.config.interceptor.AuthInterceptor;
import me.feseis.fish.logistics.service.WaybillCancelService;
import me.feseis.fish.logistics.service.WaybillExceptionService;
import me.feseis.fish.logistics.service.WaybillLifecycleService;
import me.feseis.fish.logistics.service.WaybillService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 运单接口：下单与查询。全部需要登录，账号由 AuthInterceptor 注入。
 */
@RestController
@RequestMapping("/logistics/waybill")
@RequiredArgsConstructor
public class WaybillController {

    private final WaybillService waybillService;
    private final WaybillCancelService waybillCancelService;
    private final WaybillExceptionService waybillExceptionService;
    private final WaybillLifecycleService waybillLifecycleService;

    @PostMapping
    public ResponseEntity<WaybillCreateRespVO> create(
            @RequestAttribute(AuthInterceptor.ATTR_ACCOUNT_ID) String accountId,
            @Valid @RequestBody WaybillCreateReqVO req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(waybillService.create(accountId, req));
    }

    @GetMapping("/{waybillId}")
    public WaybillDetailRespVO detail(
            @RequestAttribute(AuthInterceptor.ATTR_ACCOUNT_ID) String accountId,
            @PathVariable String waybillId) {
        return waybillService.detail(accountId, waybillId);
    }

    @GetMapping
    public List<WaybillDetailRespVO> list(
            @RequestAttribute(AuthInterceptor.ATTR_ACCOUNT_ID) String accountId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return waybillService.list(accountId, page, size);
    }

    @PostMapping("/{waybillId}/cancel")
    public WaybillOperationRespVO cancel(
            @RequestAttribute(AuthInterceptor.ATTR_ACCOUNT_ID) String accountId,
            @PathVariable String waybillId,
            @Valid @RequestBody WaybillCancelReqVO req) {
        return waybillCancelService.cancel(accountId, waybillId, req);
    }

    @PostMapping("/{waybillId}/exception")
    public WaybillOperationRespVO markException(
            @RequestAttribute(AuthInterceptor.ATTR_ACCOUNT_ID) String accountId,
            @PathVariable String waybillId,
            @Valid @RequestBody WaybillExceptionReqVO req) {
        return waybillExceptionService.mark(accountId, waybillId, req);
    }

    @PostMapping("/{waybillId}/exception/resolve")
    public WaybillOperationRespVO resolveException(
            @RequestAttribute(AuthInterceptor.ATTR_ACCOUNT_ID) String accountId,
            @PathVariable String waybillId) {
        return waybillExceptionService.resolve(accountId, waybillId);
    }

    @GetMapping("/{waybillId}/trace")
    public List<WaybillTraceRespVO> trace(
            @RequestAttribute(AuthInterceptor.ATTR_ACCOUNT_ID) String accountId,
            @PathVariable String waybillId) {
        return waybillService.trace(accountId, waybillId);
    }

    @PostMapping("/{waybillId}/pickup")
    public WaybillOperationRespVO pickup(
            @RequestAttribute(AuthInterceptor.ATTR_ACCOUNT_ID) String accountId,
            @PathVariable String waybillId) {
        return waybillLifecycleService.pickup(accountId, waybillId);
    }

    @PostMapping("/{waybillId}/deliver")
    public WaybillOperationRespVO deliver(
            @RequestAttribute(AuthInterceptor.ATTR_ACCOUNT_ID) String accountId,
            @PathVariable String waybillId,
            @Valid @RequestBody WaybillDeliverReqVO req) {
        return waybillLifecycleService.deliver(accountId, waybillId, req);
    }

    @PostMapping("/{waybillId}/deliver/fail")
    public WaybillOperationRespVO deliverFail(
            @RequestAttribute(AuthInterceptor.ATTR_ACCOUNT_ID) String accountId,
            @PathVariable String waybillId) {
        return waybillLifecycleService.deliverFail(accountId, waybillId);
    }
}
