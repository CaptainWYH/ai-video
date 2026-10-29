package com.aivideo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * AI Video Studio 后端启动类。
 *
 * <p>Mapper 接口使用 {@code @Mapper} 注解，由 MyBatis 自动扫描注册。</p>
 *
 * <p>认证完全由 JWT 过滤器负责，排除 Spring Boot 默认的
 * {@link UserDetailsServiceAutoConfiguration}，避免启动时生成无用的默认密码。</p>
 */
@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
@ConfigurationPropertiesScan
public class AiVideoApplication {

    public static void main(String[] args) {
        SpringApplication.run(AiVideoApplication.class, args);
    }
}
