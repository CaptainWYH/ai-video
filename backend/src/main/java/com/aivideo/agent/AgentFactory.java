package com.aivideo.agent;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Agent 工厂：ChatService 通过它获取当前生效的 ChatAgent。
 *
 * <p>后续可按用户/租户/会话选择不同 Agent，或在此处替换模型与工具集。</p>
 */
@Component
@RequiredArgsConstructor
public class AgentFactory {

    private final ChatAgent chatAgent;

    public ChatAgent getAgent() {
        return chatAgent;
    }
}
