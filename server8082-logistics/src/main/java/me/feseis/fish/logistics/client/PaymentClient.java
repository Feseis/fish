package me.feseis.fish.logistics.client;

import java.math.BigDecimal;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.feseis.fish.logistics.exp.BizException;
import me.feseis.fish.logistics.exp.ErrorCode;
import me.feseis.fish.payment.api.PaymentApiPaths;
import me.feseis.fish.payment.api.dto.BizTerminatedReqDTO;
import me.feseis.fish.payment.api.dto.PaymentCreateReqDTO;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * 建寄付支付单。下单与建单同事务，调用失败整笔回滚，避免出现没有支付单的待揽收运单。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentClient {

    private final RestClient paymentRestClient;

    public void createOrder(int bizType,
                            String bizId,
                            BigDecimal amount,
                            int payChannel,
                            int payMethod,
                            String payerAccountId,
                            String payerPhone) {
        try {
            paymentRestClient.post()
                    .uri(PaymentApiPaths.CREATE_INTERNAL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new PaymentCreateReqDTO(bizType, bizId, amount, payChannel, payMethod,
                            payerAccountId, payerPhone))
                    .retrieve()
                    .onStatus(HttpStatusCode::isError,
                            (request, resp) -> {
                                throw BizException.of(ErrorCode.PAYMENT_UNAVAILABLE);
                            })
                    .toBodilessEntity();
        } catch (RestClientException e) {
            log.warn("支付单创建失败 bizId={}: {}", bizId, e.getMessage());
            throw BizException.of(ErrorCode.PAYMENT_UNAVAILABLE);
        }
    }

    /** 业务终止事件：通知支付服务关闭待支付单或建全额退款。 */
    public void notifyBizTerminated(String waybillId,
                                    Integer bizType,
                                    String bizId,
                                    Long eventOutboxId) {
        try {
            paymentRestClient.post()
                    .uri(PaymentApiPaths.BIZ_TERMINATED)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new BizTerminatedReqDTO(waybillId, bizType, bizId, eventOutboxId))
                    .retrieve()
                    .onStatus(HttpStatusCode::isError,
                            (request, resp) -> {
                                throw BizException.of(ErrorCode.PAYMENT_UNAVAILABLE);
                            })
                    .toBodilessEntity();
        } catch (RestClientException e) {
            log.warn("业务终止事件投递失败 waybillId={}: {}", waybillId, e.getMessage());
            throw BizException.of(ErrorCode.PAYMENT_UNAVAILABLE);
        }
    }
}
