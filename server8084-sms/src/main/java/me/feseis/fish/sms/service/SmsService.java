package me.feseis.fish.sms.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.feseis.fish.sms.api.dto.SmsSendReqDTO;
import me.feseis.fish.sms.api.dto.SmsSendRespDTO;
import me.feseis.fish.sms.client.PboxClient;
import me.feseis.fish.sms.enums.SmsTemplate;
import me.feseis.fish.sms.exp.BizException;
import me.feseis.fish.sms.exp.ErrorCode;
import me.feseis.fish.sms.mapper.SmsRecordMapper;
import me.feseis.fish.sms.mapper.model.SmsRecordRow;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

/**
 * 短信发送编排：幂等查重 → 渲染 → 脱敏 → 投递 → 落库。
 *
 * <p>先查后发，正常调用方每次生成新 requestId，不会并发；万一并发撞唯一键，
 * 落库失败时返回已存在记录，宁可重复下发一次也不产生两条记录。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SmsService {

    private static final int RESULT_SUCCESS = 1;
    private static final int RESULT_FAILURE = 2;

    private final SmsRecordMapper smsRecordMapper;
    private final PboxClient pboxClient;

    public SmsSendRespDTO send(SmsSendReqDTO req) {
        return smsRecordMapper.findByRequestId(req.requestId())
                .map(SmsService::toResponse)
                .orElseGet(() -> doSend(req));
    }

    private SmsSendRespDTO doSend(SmsSendReqDTO req) {
        SmsTemplate template = SmsTemplate.fromCode(req.templateCode());
        template.checkParams(req.params());

        int result = RESULT_SUCCESS;
        String failReason = null;
        try {
            pboxClient.send(req.targetPhone(), template.render(req.params()), template.messageType());
        } catch (BizException e) {
            result = RESULT_FAILURE;
            failReason = e.getMessage();
        }

        SmsRecordRow row = new SmsRecordRow();
        row.setRequestId(req.requestId());
        row.setTemplateCode(template.name());
        row.setTargetPhone(req.targetPhone());
        row.setContent(template.renderMasked(req.params()));
        row.setSourceService(req.sourceService());
        row.setBizType(req.bizType());
        row.setBizId(req.bizId());
        row.setResult(result);
        row.setFailReason(failReason);

        try {
            smsRecordMapper.insert(row);
        } catch (DuplicateKeyException e) {
            log.warn("requestId 并发重复，返回已有记录 requestId={}", req.requestId());
            return smsRecordMapper.findByRequestId(req.requestId())
                    .map(SmsService::toResponse)
                    .orElseThrow(() -> BizException.of(ErrorCode.INTERNAL));
        }
        return toResponse(row);
    }

    private static SmsSendRespDTO toResponse(SmsRecordRow row) {
        return new SmsSendRespDTO(row.getSmsRecordId(), row.getRequestId(),
                row.getResult(), row.getFailReason());
    }
}
