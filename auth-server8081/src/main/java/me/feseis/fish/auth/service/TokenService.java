package me.feseis.fish.auth.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Optional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

/**
 * 令牌签发与校验。短期令牌只存 Redis 且键名用 SHA-256，原值仅出现在响应中。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TokenService {

    private static final String KEY_PREFIX = "auth:st:";
    private static final String FIELD_SEPARATOR = "|";
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final Base64.Encoder ENCODER = Base64.getUrlEncoder().withoutPadding();

    private final StringRedisTemplate redis;

    /** 32 字节随机串，Base64URL 无填充，长度 43。 */
    public String randomToken() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return ENCODER.encodeToString(bytes);
    }

    public String sha256Hex(String raw) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(raw.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 算法不可用", e);
        }
    }

    /** 写入短期令牌，值格式 sessionId|accountId|pwdVersion。 */
    public void storeAccess(String raw, SessionView view, Duration ttl) {
        String value = String.join(FIELD_SEPARATOR,
                view.sessionId(), view.accountId(), String.valueOf(view.pwdVersion()));
        redis.opsForValue().set(KEY_PREFIX + sha256Hex(raw), value, ttl);
    }

    /** 校验失败一律 fail closed，返回空而不抛异常。 */
    public Optional<SessionView> validate(String raw) {
        if (raw == null || raw.isBlank()) {
            return Optional.empty();
        }
        String key = KEY_PREFIX + sha256Hex(raw);
        String value = redis.opsForValue().get(key);
        if (value == null) {
            return Optional.empty();
        }
        String[] parts = value.split("\\|", 3);
        if (parts.length != 3) {
            log.warn("短期令牌值格式异常，删除该键 key={}", key);
            redis.delete(key);
            return Optional.empty();
        }
        try {
            return Optional.of(new SessionView(parts[0], parts[1], Integer.parseInt(parts[2])));
        } catch (NumberFormatException e) {
            log.warn("短期令牌值解析失败，删除该键 key={}", key, e);
            redis.delete(key);
            return Optional.empty();
        }
    }

    public void revoke(String raw) {
        if (raw == null || raw.isBlank()) {
            return;
        }
        redis.delete(KEY_PREFIX + sha256Hex(raw));
    }

    public record SessionView(String sessionId, String accountId, int pwdVersion) {
    }
}
