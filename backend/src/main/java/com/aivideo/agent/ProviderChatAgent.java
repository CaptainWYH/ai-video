package com.aivideo.agent;

import com.aivideo.common.BizException;
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
    public AgentOutcome chat(AgentRequest request) {
        return new AgentOutcome.Completed(modelProvider.chat(request));
    }

    @Override
    public AgentOutcome confirm(AgentConfirmRequest request) {
        throw BizException.badRequest("当前 Mock 模式不支持工具确认");
    }
}
