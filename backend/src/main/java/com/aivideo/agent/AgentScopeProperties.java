package com.aivideo.agent;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * AgentScope 配置。
 */
@Data
@ConfigurationProperties(prefix = "app.agentscope")
public class AgentScopeProperties {

    /** 默认关闭，使用 Mock ModelProvider */
    private boolean enabled = false;
    /** 形如 deepseek:deepseek-v4-flash */
    private String model = "deepseek:deepseek-v4-flash";
    /** 仅在 enabled=true 时必填，来自 DEEPSEEK_API_KEY */
    private String apiKey = "";
    /** DeepSeek OpenAI 兼容地址；不要留空，避免回落到其他厂商端点 */
    private String baseUrl = "https://api.deepseek.com";
    /** 单次模型调用超时；需小于前端 axios 的 20s */
    private Duration timeout = Duration.ofSeconds(15);
    /** ReAct 最大迭代次数，避免工具循环拖长同步请求 */
    private int maxIters = 3;
    /** 模型调用最大尝试次数；MVP 默认不自动重试 */
    private int maxAttempts = 1;
    private int maxHistoryMessages = 20;
    private String sysPrompt = "你是 AI Video Studio 的视频创作助手。";
}
