package com.aivideo.config;

import com.aivideo.agent.AgentScopeProperties;
import com.aivideo.agent.tools.VideoAssistantTools;
import io.agentscope.core.ReActAgent;
import io.agentscope.core.model.ExecutionConfig;
import io.agentscope.core.model.Model;
import io.agentscope.core.model.ModelCreationContext;
import io.agentscope.core.model.ModelRegistry;
import io.agentscope.core.permission.PermissionBehavior;
import io.agentscope.core.permission.PermissionContextState;
import io.agentscope.core.permission.PermissionMode;
import io.agentscope.core.permission.PermissionRule;
import io.agentscope.core.tool.Toolkit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

/**
 * AgentScope 配置：工具注册与真实 Agent 初始化。
 *
 * <p>默认 {@code app.agentscope.enabled=false}，此时不会创建 ReActAgent，
 * 对话走 Mock ModelProvider。启用真实模型时需设置 {@code DEEPSEEK_API_KEY}。</p>
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class AgentScopeConfig {

    private static final String DEEPSEEK_PREFIX = "deepseek:";

    private final AgentScopeProperties properties;
    private final VideoAssistantTools videoAssistantTools;

    @Bean
    public Toolkit videoAssistantToolkit() {
        Toolkit toolkit = new Toolkit();
        toolkit.registerTool(videoAssistantTools);
        return toolkit;
    }

    /**
     * 演示用权限上下文：generateVideoClip 调用前必须由用户确认，其他工具直接放行。
     *
     * <p>默认 DEFAULT 模式在无规则时不会触发 ASK，因此这里用 BYPASS + 显式 ASK 规则。</p>
     */
    @Bean
    public PermissionContextState toolConfirmPermissionContext() {
        return PermissionContextState.builder()
                .mode(PermissionMode.BYPASS)
                .addAskRule("generateVideoClip",
                        new PermissionRule("generateVideoClip", "",
                                PermissionBehavior.ASK, "user"))
                .build();
    }

    @Bean
    @ConditionalOnProperty(name = "app.agentscope.enabled", havingValue = "true")
    public ReActAgent videoAssistantAgent(Toolkit videoAssistantToolkit) {
        validateProperties();

        // 交给 ModelRegistry 的 deepseek provider 处理 formatter 与默认地址，
        // 避免自行构造 OpenAIChatModel 时遗漏 DeepSeek 的 reasoning_content 兼容细节。
        Model model = ModelRegistry.resolve(
                properties.getModel().trim(),
                ModelCreationContext.builder()
                        .apiKey(properties.getApiKey().trim())
                        .baseUrl(properties.getBaseUrl().trim())
                        .stream(properties.isStream())
                        .build());

        log.info("AgentScope 真实模型: model={}, baseUrl={}, timeout={}, apiKey=******",
                properties.getModel(), properties.getBaseUrl(), properties.getTimeout());

        return ReActAgent.builder()
                .name("VideoAssistantAgent")
                .sysPrompt(properties.getSysPrompt())
                .model(model)
                .toolkit(videoAssistantToolkit)
                .maxIters(properties.getMaxIters())
                .modelExecutionConfig(ExecutionConfig.builder()
                        .timeout(properties.getTimeout())
                        .maxAttempts(properties.getMaxAttempts())
                        .build())
                .build();
    }

    private void validateProperties() {
        if (!StringUtils.hasText(properties.getModel())
                || !properties.getModel().trim().startsWith(DEEPSEEK_PREFIX)) {
            throw new IllegalStateException(
                    "app.agentscope.model 需以 deepseek: 开头，例如 deepseek:deepseek-v4-flash，当前值: "
                            + properties.getModel());
        }
        if (!StringUtils.hasText(properties.getApiKey())) {
            throw new IllegalStateException(
                    "AGENTSCOPE_ENABLED=true 但 DEEPSEEK_API_KEY 为空。"
                            + "请设置环境变量 DEEPSEEK_API_KEY，"
                            + "或设置 AGENTSCOPE_ENABLED=false 使用 Mock 模式。");
        }
        if (!StringUtils.hasText(properties.getBaseUrl())) {
            throw new IllegalStateException(
                    "app.agentscope.base-url 不能为空，默认应为 https://api.deepseek.com");
        }
        if (properties.getTimeout() == null
                || properties.getTimeout().isZero()
                || properties.getTimeout().isNegative()) {
            throw new IllegalStateException("app.agentscope.timeout 必须为正数，例如 PT15S");
        }
        if (properties.getMaxIters() < 1) {
            throw new IllegalStateException("app.agentscope.max-iters 必须大于 0");
        }
        if (properties.getMaxAttempts() < 1) {
            throw new IllegalStateException("app.agentscope.max-attempts 必须大于 0");
        }
    }
}
