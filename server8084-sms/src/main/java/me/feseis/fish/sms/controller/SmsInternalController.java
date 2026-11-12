package me.feseis.fish.sms.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import me.feseis.fish.sms.api.SmsApiPaths;
import me.feseis.fish.sms.api.dto.PhoneExistsRespDTO;
import me.feseis.fish.sms.api.dto.SmsSendReqDTO;
import me.feseis.fish.sms.api.dto.SmsSendRespDTO;
import me.feseis.fish.sms.service.SmsPhoneService;
import me.feseis.fish.sms.service.SmsService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 短信内部接口，只给其他服务调用。内部令牌另行一轮接入。
 */
@RestController
@RequiredArgsConstructor
public class SmsInternalController {

    private final SmsService smsService;
    private final SmsPhoneService smsPhoneService;

    @PostMapping(SmsApiPaths.SEND)
    public SmsSendRespDTO send(@Valid @RequestBody SmsSendReqDTO req) {
        return smsService.send(req);
    }

    @GetMapping(SmsApiPaths.PHONE_EXISTS)
    public PhoneExistsRespDTO phoneExists(@RequestParam("phone") String phone) {
        return smsPhoneService.exists(phone);
    }
}
