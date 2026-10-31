package com.aivideo.config;

import com.aivideo.agent.AgentScopeProperties;
import com.alibaba.druid.pool.DruidDataSource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.data.redis.RedisProperties;
import org.springframework.core.env.Environment;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.sql.DataSource;

/**
 * 启动后打印实际生效的 MySQL / Redis 配置，便于确认外部数据库是否被正确加载。
 *
 * <p>密码只输出占位符；Redis 会额外做一次 PING，失败仅告警，不阻断启动。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StartupInfoRunner implements ApplicationRunner {

    private final DataSource dataSource;
    private final RedisProperties redisProperties;
    private final RedisConnectionFactory redisConnectionFactory;
    private final AgentScopeProperties agentScopeProperties;
    private final Environment environment;

    @Override
    public void run(ApplicationArguments args) {
        String[] profiles = environment.getActiveProfiles();
        log.info("========== 启动配置检查 ==========");
        log.info("Active profiles : {}", profiles.length == 0 ? "default" : String.join(",", profiles));

        if (dataSource instanceof DruidDataSource druid) {
            log.info("MySQL  [Druid] : url={}", druid.getUrl());
            log.info("MySQL  [Druid] : username={}, driver={}", druid.getUsername(), druid.getDriverClassName());
            log.info("MySQL  [Druid] : initialSize={}, minIdle={}, maxActive={}, maxWait={}ms",
                    druid.getInitialSize(), druid.getMinIdle(), druid.getMaxActive(), druid.getMaxWait());
            log.info("MySQL  [Druid] : validationQuery={}, filters={}",
                    druid.getValidationQuery(),
                    druid.getProxyFilters().stream()
                            .map(filter -> filter.getClass().getSimpleName())
                            .toList());
        } else {
            log.info("MySQL  datasource : {}", dataSource.getClass().getName());
        }

        log.info("Redis          : host={}, port={}, database={}, ssl={}, username={}, password={}",
                redisProperties.getHost(),
                redisProperties.getPort(),
                redisProperties.getDatabase(),
                redisProperties.getSsl().isEnabled(),
                StringUtils.hasText(redisProperties.getUsername()) ? redisProperties.getUsername() : "<none>",
                StringUtils.hasText(redisProperties.getPassword()) ? "******" : "<none>");

        String tokenStore = environment.getProperty("app.token-store", "redis");
        log.info("TokenStore     : {}", tokenStore);
        if ("redis".equalsIgnoreCase(tokenStore)) {
            pingRedis();
        } else {
            log.info("Redis          : 当前 TokenStore 为 {}，跳过 PING", tokenStore);
        }

        if (agentScopeProperties.isEnabled()) {
            log.info("AgentScope     : enabled=true, model={}, baseUrl={}, timeout={}",
                    agentScopeProperties.getModel(),
                    agentScopeProperties.getBaseUrl(),
                    agentScopeProperties.getTimeout());
        } else {
            log.info("AgentScope     : enabled=false（Mock ModelProvider）");
        }
        log.info("==================================");
    }

    private void pingRedis() {
        try (RedisConnection connection = redisConnectionFactory.getConnection()) {
            String pong = connection.ping();
            log.info("Redis          : PING={}", pong);
        } catch (Exception exception) {
            Throwable root = exception;
            while (root.getCause() != null && root.getCause() != root) {
                root = root.getCause();
            }
            log.warn("Redis          : PING 失败（登录/登出等依赖 Redis 的操作会受影响）：{}: {}",
                    root.getClass().getSimpleName(), root.getMessage());
        }
    }
}
