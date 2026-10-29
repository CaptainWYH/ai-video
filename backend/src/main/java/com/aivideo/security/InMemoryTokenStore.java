package com.aivideo.security;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 内存 TokenStore，仅用于 dev profile（无 Redis）本地演示。
 * 重启后登录态丢失，生产环境请使用 {@link RedisTokenStore}。
 */
@Component
@ConditionalOnProperty(name = "app.token-store", havingValue = "memory")
public class InMemoryTokenStore implements TokenStore {

    private record Entry(String value, long expiresAtMillis) {
        boolean expired() {
            return System.currentTimeMillis() >= expiresAtMillis;
        }
    }

    private final Map<String, Entry> refreshTokens = new ConcurrentHashMap<>();
    private final Map<String, Entry> blacklist = new ConcurrentHashMap<>();

    @Override
    public void saveRefreshToken(String jti, Long userId, Duration ttl) {
        refreshTokens.put(jti, new Entry(String.valueOf(userId), expiresAt(ttl)));
    }

    @Override
    public Long getRefreshTokenUserId(String jti) {
        Entry entry = refreshTokens.get(jti);
        if (entry == null) {
            return null;
        }
        if (entry.expired()) {
            refreshTokens.remove(jti);
            return null;
        }
        return Long.valueOf(entry.value());
    }

    @Override
    public void deleteRefreshToken(String jti) {
        refreshTokens.remove(jti);
    }

    @Override
    public void blacklistAccessToken(String jti, Duration ttl) {
        if (!ttl.isZero() && !ttl.isNegative()) {
            blacklist.put(jti, new Entry("1", expiresAt(ttl)));
        }
    }

    @Override
    public boolean isAccessTokenBlacklisted(String jti) {
        Entry entry = blacklist.get(jti);
        if (entry == null) {
            return false;
        }
        if (entry.expired()) {
            blacklist.remove(jti);
            return false;
        }
        return true;
    }

    private long expiresAt(Duration ttl) {
        return System.currentTimeMillis() + ttl.toMillis();
    }
}
