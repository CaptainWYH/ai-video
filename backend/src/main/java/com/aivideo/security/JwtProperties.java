package com.aivideo.security;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * JWT 配置。敏感值通过环境变量注入。
 */
@Data
@ConfigurationProperties(prefix = "app.jwt")
public class JwtProperties {

    /** HMAC 密钥，长度至少 32 字节 */
    private String secret;
    private String issuer = "ai-video-studio";
    private Duration accessTokenTtl = Duration.ofMinutes(30);
    private Duration refreshTokenTtl = Duration.ofDays(7);
}
