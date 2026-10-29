package com.aivideo.security;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Redis TokenStore：refresh token 白名单 + access token 黑名单。
 */
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.token-store", havingValue = "redis", matchIfMissing = true)
public class RedisTokenStore implements TokenStore {

    private static final String REFRESH_PREFIX = "auth:refresh:";
    private static final String BLACKLIST_PREFIX = "auth:blacklist:";

    private final StringRedisTemplate redisTemplate;

    @Override
    public void saveRefreshToken(String jti, Long userId, Duration ttl) {
        redisTemplate.opsForValue().set(REFRESH_PREFIX + jti, String.valueOf(userId), ttl);
    }

    @Override
    public Long getRefreshTokenUserId(String jti) {
        String value = redisTemplate.opsForValue().get(REFRESH_PREFIX + jti);
        return value == null ? null : Long.valueOf(value);
    }

    @Override
    public void deleteRefreshToken(String jti) {
        redisTemplate.delete(REFRESH_PREFIX + jti);
    }

    @Override
    public void blacklistAccessToken(String jti, Duration ttl) {
        if (!ttl.isZero() && !ttl.isNegative()) {
            redisTemplate.opsForValue().set(BLACKLIST_PREFIX + jti, "1", ttl);
        }
    }

    @Override
    public boolean isAccessTokenBlacklisted(String jti) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(BLACKLIST_PREFIX + jti));
    }
}
