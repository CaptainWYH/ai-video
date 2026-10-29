package com.aivideo.agent;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * AgentScope 配置。
 */
@Data
@ConfigurationProperties(prefix = "app.agentscope")
public class AgentScopeProperties {

    /** 默认关闭，使用 Mock ModelProvider */
    private boolean enabled = false;
    /** 例：dashscope:qwen-plus、openai:gpt-4.1-mini */
    private String model = "dashscope:qwen-plus";
    private int maxHistoryMessages = 20;
    private String sysPrompt = "你是 AI Video Studio 的视频创作助手。";
}
