package me.feseis.fish.auth.service;

import java.security.SecureRandom;

import lombok.RequiredArgsConstructor;
import me.feseis.fish.auth.config.properties.AuthProperties;
import me.feseis.fish.auth.exp.AuthException;
import me.feseis.fish.auth.exp.ErrorCode;
import me.feseis.fish.auth.sender.SmsSender;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

/**
 * 验证码的生成、频控与校验。
 *
 * <p>key 设计：
 * <ul>
 *   <li>{@code auth:sms:code:{phone}} 验证码，校验成功即删除，防重放</li>
 *   <li>{@code auth:sms:cd:{phone}} 重发冷却，命中即拒绝</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class SmsCodeService {

    private static final String CODE_KEY = "auth:sms:code:";
    private static final String CD_KEY = "auth:sms:cd:";
    private static final SecureRandom RANDOM = new SecureRandom();

    private final StringRedisTemplate redis;
    private final SmsSender smsSender;
    private final AuthProperties props;

    /** 生成验证码并委托 SmsSender 发送；冷却期内重复请求直接拒绝。 */
    public void sendCode(String phone) {
        if (Boolean.TRUE.equals(redis.hasKey(CD_KEY + phone))) {
            throw AuthException.of(ErrorCode.RATE_LIMITED);
        }
        String code = String.format("%06d", RANDOM.nextInt(1_000_000));
        redis.opsForValue().set(CODE_KEY + phone, code, props.sms().codeTtl());
        redis.opsForValue().set(CD_KEY + phone, "1", props.sms().resendInterval());
        try {
            smsSender.send(phone, code);
        } catch (RuntimeException e) {
            // 投递失败即回滚：不能让用户对着收不到的码，还被冷却锁住
            redis.delete(CODE_KEY + phone);
            redis.delete(CD_KEY + phone);
            throw e;
        }
    }

    /** 校验验证码，成功即删除；失败不删除，保留到自然过期。 */
    public void verify(String phone, String code) {
        String saved = redis.opsForValue().get(CODE_KEY + phone);
        if (saved == null) {
            throw AuthException.of(ErrorCode.SMS_EXPIRED);
        }
        if (!saved.equals(code)) {
            throw AuthException.of(ErrorCode.SMS_WRONG);
        }
        redis.delete(CODE_KEY + phone);
    }
}
