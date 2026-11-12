package me.feseis.fish.payment.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import me.feseis.fish.payment.api.PaymentApiPaths;
import me.feseis.fish.payment.api.dto.BizTerminatedReqDTO;
import me.feseis.fish.payment.api.dto.BizTerminatedRespDTO;
import me.feseis.fish.payment.api.dto.PaymentCreateReqDTO;
import me.feseis.fish.payment.api.dto.PaymentCreateRespDTO;
import me.feseis.fish.payment.service.PaymentService;
import me.feseis.fish.payment.service.PaymentSettlementService;
import me.feseis.fish.payment.vo.PaymentRespVO;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * 支付内部接口。只给其他服务调用，与对外接口分路径、分 DTO。
 */
@RestController
@RequiredArgsConstructor
public class InternalPaymentController {

    private final PaymentService paymentService;
    private final PaymentSettlementService paymentSettlementService;

    @PostMapping(PaymentApiPaths.CREATE_INTERNAL)
    public ResponseEntity<PaymentCreateRespDTO> create(@Valid @RequestBody PaymentCreateReqDTO req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(toCreateResp(paymentService.create(req)));
    }

    @PostMapping(PaymentApiPaths.BIZ_TERMINATED)
    public BizTerminatedRespDTO bizTerminated(@Valid @RequestBody BizTerminatedReqDTO req) {
        return paymentSettlementService.settle(req);
    }

    private static PaymentCreateRespDTO toCreateResp(PaymentRespVO vo) {
        return new PaymentCreateRespDTO(
                vo.paymentId(),
                vo.paymentNo(),
                vo.bizType(),
                vo.bizId(),
                vo.amount(),
                vo.payChannel(),
                vo.status(),
                vo.payToken(),
                vo.qrCode(),
                vo.payRemark(),
                vo.expireAt(),
                vo.paidAt(),
                vo.closedAt(),
                vo.createdAt());
    }
}
