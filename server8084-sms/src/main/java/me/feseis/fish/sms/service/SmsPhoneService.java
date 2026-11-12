package me.feseis.fish.sms.service;

import lombok.RequiredArgsConstructor;
import me.feseis.fish.sms.api.dto.PhoneExistsRespDTO;
import me.feseis.fish.sms.client.PboxClient;
import org.springframework.stereotype.Service;

/**
 * 手机号沙盒登记查询。查询本身不改状态，失败直接抛给调用方。
 */
@Service
@RequiredArgsConstructor
public class SmsPhoneService {

    private final PboxClient pboxClient;

    public PhoneExistsRespDTO exists(String phone) {
        return new PhoneExistsRespDTO(phone, pboxClient.exists(phone));
    }
}
