package com.aivideo.config;

import com.aivideo.agent.AgentScopeProperties;
import com.aivideo.agent.tools.VideoAssistantTools;
import io.agentscope.core.ReActAgent;
import io.agentscope.core.tool.Toolkit;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * AgentScope 配置：工具注册与真实 Agent 初始化。
 *
 * <p>默认 {@code app.agentscope.enabled=false}，此时不会创建 ReActAgent，
 * 对话走 Mock ModelProvider。启用真实模型时需设置对应厂商 API Key 环境变量。</p>
 */
@Configuration
@RequiredArgsConstructor
public class AgentScopeConfig {

    private final AgentScopeProperties properties;

    @Bean
    public Toolkit videoAssistantToolkit() {
        Toolkit toolkit = new Toolkit();
        toolkit.registerTool(new VideoAssistantTools());
        return toolkit;
    }

    @Bean
    @ConditionalOnProperty(name = "app.agentscope.enabled", havingValue = "true")
    public ReActAgent videoAssistantAgent(Toolkit videoAssistantToolkit) {
        return ReActAgent.builder()
                .name("VideoAssistantAgent")
                .sysPrompt(properties.getSysPrompt())
                .model(properties.getModel())
                .toolkit(videoAssistantToolkit)
                .build();
    }
}
