package me.feseis.fish.auth.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth/health")
@RequiredArgsConstructor
public class HealthController {

    private final JdbcTemplate jdbcTemplate;
    private final StringRedisTemplate stringRedisTemplate;

    @GetMapping
    public ResponseEntity<HealthStatus> health() {
        HealthStatus status = new HealthStatus(checkDatabase(), checkRedis());
        if (status.database() && status.redis()) {
            return ResponseEntity.ok(status);
        }
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(status);
    }

    private boolean checkDatabase() {
        try {
            jdbcTemplate.queryForObject("SELECT 1", Integer.class);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private boolean checkRedis() {
        RedisConnection connection = null;
        try {
            connection = stringRedisTemplate.getConnectionFactory().getConnection();
            connection.ping();
            return true;
        } catch (Exception e) {
            return false;
        } finally {
            if (connection != null) {
                connection.close();
            }
        }
    }

    public record HealthStatus(boolean database, boolean redis) {
    }
}
