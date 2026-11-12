package me.feseis.fish.sms.api.dto;

/**
 * 发送结果。result 1成功 2失败；失败同样返回 200，便于幂等重放拿到一致结果。
 */
public record SmsSendRespDTO(
        Long smsRecordId,
        String requestId,
        Integer result,
        String failReason
) {
}
