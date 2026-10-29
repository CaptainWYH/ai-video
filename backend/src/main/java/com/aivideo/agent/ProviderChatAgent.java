package com.aivideo.agent;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * 基于 {@link ModelProvider} 的 ChatAgent（Mock 模式）。
 */
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.agentscope.enabled", havingValue = "false", matchIfMissing = true)
public class ProviderChatAgent implements ChatAgent {

    private final ModelProvider modelProvider;

    @Override
    public AgentReply chat(AgentRequest request) {
        return modelProvider.chat(request);
    }
}
