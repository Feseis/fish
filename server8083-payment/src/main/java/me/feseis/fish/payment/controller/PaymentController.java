package me.feseis.fish.payment.controller;

import java.util.List;

import lombok.RequiredArgsConstructor;
import me.feseis.fish.payment.service.PaymentService;
import me.feseis.fish.payment.vo.PaymentRespVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 支付对外接口：查询、模拟支付、渠道查单、关闭。建单已移到内部接口。
 */
@RestController
@RequestMapping("/payment/orders")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @GetMapping("/{paymentId}")
    public PaymentRespVO detail(@PathVariable String paymentId) {
        return paymentService.findById(paymentId);
    }

    @GetMapping
    public List<PaymentRespVO> listByBiz(@RequestParam Integer bizType,
                                         @RequestParam String bizId) {
        return paymentService.findByBiz(bizType, bizId);
    }

    @PostMapping("/{paymentId}/mock-pay")
    public PaymentRespVO mockPay(@PathVariable String paymentId) {
        return paymentService.mockPay(paymentId);
    }

    @PostMapping("/{paymentId}/sync")
    public PaymentRespVO sync(@PathVariable String paymentId) {
        return paymentService.syncFromChannel(paymentId);
    }

    @PostMapping("/{paymentId}/close")
    public PaymentRespVO close(@PathVariable String paymentId) {
        return paymentService.close(paymentId);
    }
}
