package com.aivideo.agent;

import com.aivideo.common.BizException;
import com.aivideo.common.ResultCode;
import io.agentscope.core.ReActAgent;
import io.agentscope.core.agent.RuntimeContext;
import io.agentscope.core.event.AgentEvent;
import io.agentscope.core.event.AgentResultEvent;
import io.agentscope.core.event.ConfirmResult;
import io.agentscope.core.event.RequireUserConfirmEvent;
import io.agentscope.core.event.ToolCallEndEvent;
import io.agentscope.core.event.ToolCallStartEvent;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.ToolUseBlock;
import io.agentscope.core.message.UserMessage;
import io.agentscope.core.permission.PermissionContextState;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;

/**
 * 基于 AgentScope ReActAgent 的真实 Agent 实现。
 *
 * <p>仅当 {@code app.agentscope.enabled=true} 时装配。通过 streamEvents 消费
 * {@code RequireUserConfirmEvent}，在工具执行前暂停；用户确认后携带
 * {@link Msg#METADATA_CONFIRM_RESULTS} 在同一 RuntimeContext 槽位上恢复执行。</p>
 */
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.agentscope.enabled", havingValue = "true")
public class AgentScopeChatAgent implements ChatAgent {

    private final ReActAgent videoAssistantAgent;
    private final AgentScopeProperties properties;
    private final PermissionContextState toolConfirmPermissionContext;

    @Override
    public AgentOutcome chat(AgentRequest request) {
        String userId = String.valueOf(request.userId());
        String sessionId = String.valueOf(request.sessionId());
        RuntimeContext context = RuntimeContext.builder()
                .userId(userId)
                .sessionId(sessionId)
                .build();
        installPermissionContext(userId, sessionId);

        return invoke(
                List.of(new UserMessage(buildPrompt(request))),
                context,
                request.content(),
                List.of(),
                false,
                false);
    }

    @Override
    public AgentOutcome confirm(AgentConfirmRequest request) {
        String userId = String.valueOf(request.userId());
        String sessionId = String.valueOf(request.sessionId());
        RuntimeContext context = RuntimeContext.builder()
                .userId(userId)
                .sessionId(sessionId)
                .build();
        installPermissionContext(userId, sessionId);

        List<ConfirmResult> results = request.toolCalls().stream()
                .map(toolCall -> new ConfirmResult(request.approved(), toToolUseBlock(toolCall)))
                .toList();
        Msg confirmMessage = UserMessage.builder()
                .metadata(Map.of(Msg.METADATA_CONFIRM_RESULTS, results))
                .build();

        return invoke(
                List.of(confirmMessage),
                context,
                "",
                request.toolCalls(),
                request.approved(),
                true);
    }

    @Override
    public void reset(Long userId, Long sessionId) {
        videoAssistantAgent.clearStateCache(String.valueOf(userId), String.valueOf(sessionId));
    }

    private AgentOutcome invoke(List<Msg> messages,
                                RuntimeContext context,
                                String originalContent,
                                List<AgentToolCall> submittedToolCalls,
                                boolean approved,
                                boolean fromConfirm) {
        try {
            // 必须消费到 onComplete，避免提前取消导致 AgentState 未保存。
            List<AgentEvent> events = videoAssistantAgent
                    .streamEvents(messages, context)
                    .collectList()
                    .block(properties.getTimeout().plusSeconds(2));

            if (events == null) {
                throw new BizException(ResultCode.MODEL_UNAVAILABLE, "模型未返回事件流，请重试");
            }

            String resultText = lastResultText(events);
            RequireUserConfirmEvent confirmEvent = lastConfirmEvent(events);
            if (confirmEvent != null && !confirmEvent.getToolCalls().isEmpty()) {
                List<AgentToolCall> pending = confirmEvent.getToolCalls().stream()
                        .map(block -> new AgentToolCall(block.getId(), block.getName(), block.getInput()))
                        .toList();
                return new AgentOutcome.AwaitingConfirmation(
                        confirmEvent.getReplyId(), pending, resultText);
            }

            String toolName = resolveToolName(events, originalContent, submittedToolCalls);
            String text = resultText == null ? "" : resultText.trim();
            if (!StringUtils.hasText(text)) {
                if (!fromConfirm) {
                    throw new BizException(ResultCode.MODEL_UNAVAILABLE, "模型未返回有效回复，请重试");
                }
                text = approved
                        ? "工具调用已完成（当前为占位实现）。"
                        : "已拒绝工具调用，未执行该操作。";
            }

            // 确认恢复后的回复不再触发前端的旧视频任务自动生成。
            boolean videoIntent = !fromConfirm && StringUtils.hasText(toolName);
            return new AgentOutcome.Completed(new AgentReply(text, videoIntent, toolName));
        } catch (BizException ex) {
            throw ex;
        } catch (IllegalStateException ex) {
            if (fromConfirm) {
                throw BizException.conflict("工具确认状态已失效，请重新发送消息");
            }
            throw ModelErrorTranslator.translate(ex);
        } catch (RuntimeException ex) {
            throw ModelErrorTranslator.translate(ex);
        }
    }

    private void installPermissionContext(String userId, String sessionId) {
        // 权限上下文是 per-slot 状态，必须安装在与本次调用相同的 userId/sessionId 槽位上。
        videoAssistantAgent.replacePermissionContext(userId, sessionId, toolConfirmPermissionContext);
    }

    private String lastResultText(List<AgentEvent> events) {
        return events.stream()
                .filter(AgentResultEvent.class::isInstance)
                .map(AgentResultEvent.class::cast)
                .reduce((first, second) -> second)
                .map(AgentResultEvent::getResult)
                .map(Msg::getTextContent)
                .orElse("");
    }

    private RequireUserConfirmEvent lastConfirmEvent(List<AgentEvent> events) {
        return events.stream()
                .filter(RequireUserConfirmEvent.class::isInstance)
                .map(RequireUserConfirmEvent.class::cast)
                .reduce((first, second) -> second)
                .orElse(null);
    }

    private String resolveToolName(List<AgentEvent> events,
                                   String originalContent,
                                   List<AgentToolCall> submittedToolCalls) {
        String fromEvents = events.stream()
                .filter(ToolCallEndEvent.class::isInstance)
                .map(ToolCallEndEvent.class::cast)
                .map(ToolCallEndEvent::getToolCallName)
                .filter(StringUtils::hasText)
                .reduce((first, second) -> second)
                .orElseGet(() -> events.stream()
                        .filter(ToolCallStartEvent.class::isInstance)
                        .map(ToolCallStartEvent.class::cast)
                        .map(ToolCallStartEvent::getToolCallName)
                        .filter(StringUtils::hasText)
                        .reduce((first, second) -> second)
                        .orElse(null));
        if (StringUtils.hasText(fromEvents)) {
            return fromEvents;
        }
        if (submittedToolCalls != null && !submittedToolCalls.isEmpty()) {
            return submittedToolCalls.get(0).name();
        }
        return detectTool(originalContent);
    }

    private ToolUseBlock toToolUseBlock(AgentToolCall toolCall) {
        // 必须原样保留 id/name/input：恢复时 AgentScope 会用它替换 context 中的原始 block。
        return ToolUseBlock.builder()
                .id(toolCall.id())
                .name(toolCall.name())
                .input(toolCall.input())
                .build();
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
     * 意图识别的兜底：事件流中没有工具调用事件时按关键词判断。
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
