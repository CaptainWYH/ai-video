package com.aivideo.agent;

import io.agentscope.core.ReActAgent;
import io.agentscope.core.agent.RuntimeContext;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.UserMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 基于 AgentScope ReActAgent 的真实 Agent 实现。
 *
 * <p>仅当 {@code app.agentscope.enabled=true} 时装配。历史和上下文拼接方式为骨架实现，
 * 后续可替换为 AgentScope 的 session/memory 能力。</p>
 */
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.agentscope.enabled", havingValue = "true")
public class AgentScopeChatAgent implements ChatAgent {

    private final ReActAgent videoAssistantAgent;

    @Override
    public AgentReply chat(AgentRequest request) {
        RuntimeContext context = RuntimeContext.builder()
                .userId(String.valueOf(request.userId()))
                .sessionId(String.valueOf(request.sessionId()))
                .build();

        Msg reply = videoAssistantAgent
                .call(List.of(new UserMessage(buildPrompt(request))), context)
                .block();

        String text = reply == null || reply.getTextContent() == null
                ? "" : reply.getTextContent();
        String toolName = detectTool(request.content());
        return new AgentReply(text, toolName != null, toolName);
    }

    private String buildPrompt(AgentRequest request) {
        if (request.history() == null || request.history().isEmpty()) {
            return request.content();
        }
        StringBuilder builder = new StringBuilder("以下是历史对话：\n");
        request.history().forEach(message ->
                builder.append(message.role()).append(": ").append(message.content()).append('\n'));
        builder.append("\n当前用户消息：\n").append(request.content());
        return builder.toString();
    }

    /**
     * 占位意图识别。真实实现应读取 Agent 的工具调用事件。
     * TODO: 从 AgentScope 事件流中提取 tool call 并持久化。
     */
    private String detectTool(String content) {
        if (content == null) {
            return null;
        }
        if (content.contains("分镜") || content.contains("storyboard")) {
            return "generateStoryboard";
        }
        if (content.contains("视频") || content.contains("短片") || content.contains("镜头")) {
            return "generateVideoClip";
        }
        return null;
    }
}
