package me.feseis.fish.logistics.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.feseis.fish.logistics.client.PaymentClient;
import me.feseis.fish.logistics.mapper.EventOutboxMapper;
import me.feseis.fish.logistics.mapper.model.EventOutboxRow;
import me.feseis.fish.logistics.vo.outbox.OutboxDispatchRespVO;
import org.springframework.stereotype.Service;

/**
 * 跨库事件投递。当前只处理 event_type=3 业务终止 → payment。
 * 手动触发接口先落地，定时任务与退避策略见待办 E-09。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OutboxDispatchService {

    private static final int TARGET_PAYMENT = 3;
    private static final int EVENT_TYPE_BIZ_TERMINATED = 3;
    private static final int MAX_RETRY = 3;
    private static final int STATUS_DELIVERED = 2;
    private static final int STATUS_RETRY = 3;
    private static final int STATUS_ABANDONED = 4;

    private final EventOutboxMapper eventOutboxMapper;
    private final PaymentClient paymentClient;

    public OutboxDispatchRespVO dispatch(int limit) {
        List<EventOutboxRow> pending = eventOutboxMapper.findPending(TARGET_PAYMENT, limit);
        int delivered = 0;
        int retried = 0;
        int abandoned = 0;
        List<OutboxDispatchRespVO.Item> details = new ArrayList<>();

        for (EventOutboxRow row : pending) {
            try {
                handle(row);
                eventOutboxMapper.markDelivered(row.getEventOutboxId());
                delivered++;
                details.add(new OutboxDispatchRespVO.Item(row.getEventOutboxId(), STATUS_DELIVERED, null));
            } catch (RuntimeException e) {
                String error = truncate(e.getMessage());
                log.warn("事件投递失败 eventId={} waybillId={}: {}",
                        row.getEventOutboxId(), row.getWaybillId(), error);
                int retryCount = row.getRetryCount() == null ? 1 : row.getRetryCount() + 1;
                if (retryCount >= MAX_RETRY) {
                    eventOutboxMapper.markAbandoned(row.getEventOutboxId(), error);
                    abandoned++;
                    details.add(new OutboxDispatchRespVO.Item(
                            row.getEventOutboxId(), STATUS_ABANDONED, error));
                } else {
                    eventOutboxMapper.markRetry(row.getEventOutboxId(), retryCount,
                            LocalDateTime.now().plusMinutes(retryCount), error);
                    retried++;
                    details.add(new OutboxDispatchRespVO.Item(
                            row.getEventOutboxId(), STATUS_RETRY, error));
                }
            }
        }

        return new OutboxDispatchRespVO(pending.size(), delivered, retried, abandoned, details);
    }

    private void handle(EventOutboxRow row) {
        if (!Integer.valueOf(EVENT_TYPE_BIZ_TERMINATED).equals(row.getEventType())) {
            throw new IllegalStateException("未实现的事件类型：" + row.getEventType());
        }
        paymentClient.notifyBizTerminated(row.getWaybillId(), row.getBizType(), row.getBizId(),
                row.getEventOutboxId());
    }

    private static String truncate(String message) {
        if (message == null || message.isBlank()) {
            return "未知错误";
        }
        return message.length() <= 255 ? message : message.substring(0, 255);
    }
}
