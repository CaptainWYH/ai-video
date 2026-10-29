package com.aivideo.security;

import java.time.Duration;

/**
 * Refresh Token 与 Access Token 黑名单存储抽象。
 * 默认 Redis 实现；dev profile 可使用内存实现。
 */
public interface TokenStore {

    void saveRefreshToken(String jti, Long userId, Duration ttl);

    Long getRefreshTokenUserId(String jti);

    void deleteRefreshToken(String jti);

    void blacklistAccessToken(String jti, Duration ttl);

    boolean isAccessTokenBlacklisted(String jti);
}
