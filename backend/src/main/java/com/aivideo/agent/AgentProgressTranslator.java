package com.aivideo.agent;

import io.agentscope.core.event.AgentResultEvent;
import io.agentscope.core.event.AgentStartEvent;
import io.agentscope.core.event.AllToolsDeniedEvent;
import io.agentscope.core.event.ExceedMaxItersEvent;
import io.agentscope.core.event.ModelCallEndEvent;
import io.agentscope.core.event.ModelCallStartEvent;
import io.agentscope.core.event.RequestStopEvent;
import io.agentscope.core.event.RequireUserConfirmEvent;
import io.agentscope.core.event.TextBlockEndEvent;
import io.agentscope.core.event.TextBlockStartEvent;
import io.agentscope.core.event.ThinkingBlockDeltaEvent;
import io.agentscope.core.event.ThinkingBlockEndEvent;
import io.agentscope.core.event.ThinkingBlockStartEvent;
import io.agentscope.core.event.ToolCallDeltaEvent;
import io.agentscope.core.event.ToolCallEndEvent;
import io.agentscope.core.event.ToolCallStartEvent;
import io.agentscope.core.event.ToolResultEndEvent;
import io.agentscope.core.event.ToolResultStartEvent;
import io.agentscope.core.event.ToolResultTextDeltaEvent;
import io.agentscope.core.event.AgentEvent;
import io.agentscope.core.message.ToolResultState;
import io.agentscope.core.message.ToolUseBlock;
import io.agentscope.core.model.ChatUsage;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.List;

/**
 * 将 AgentScope 事件流翻译成 Agent 推理步骤。纯翻译，不持有持久化状态，
 * 步骤聚合与截断由 {@link AgentTraceCollector} 负责。
 */
public class AgentProgressTranslator {

    private static final String TRACE_STATUS_START = "status:start";
    private static final String TRACE_STATUS_RESULT = "status:result";
    private static final String TRACE_STATUS_MAX_ITERS = "status:max-iters";
    private static final String TRACE_STATUS_REQUEST_STOP = "status:request-stop";
    private static final String TRACE_STATUS_TOOLS_DENIED = "status:tools-denied";

    private final AgentProgressListener listener;

    public AgentProgressTranslator(AgentProgressListener listener) {
        this.listener = listener == null ? AgentProgressListener.NOOP : listener;
    }

    public void accept(AgentEvent event) {
        if (event == null) {
            return;
        }
        if (event instanceof AgentStartEvent) {
            emit(step(TRACE_STATUS_START, AgentTraceType.STATUS,
                    "开始分析需求", AgentTraceStatus.RUNNING));
        } else if (event instanceof ModelCallStartEvent e) {
            emit(step(modelId(e.getReplyId()), AgentTraceType.STATUS,
                    "模型推理中", AgentTraceStatus.RUNNING));
        } else if (event instanceof ModelCallEndEvent e) {
            emit(stepWithDetail(modelId(e.getReplyId()), AgentTraceType.STATUS,
                    "模型推理完成", AgentTraceStatus.SUCCESS, usageText(e.getUsage())));
        } else if (event instanceof ThinkingBlockStartEvent e) {
            emit(step(thinkingId(e.getReplyId(), e.getBlockId()), AgentTraceType.THINKING,
                    "正在推理", AgentTraceStatus.RUNNING));
        } else if (event instanceof ThinkingBlockDeltaEvent e) {
            if (StringUtils.hasText(e.getDelta())) {
                emitDelta(deltaStep(thinkingId(e.getReplyId(), e.getBlockId()),
                        AgentTraceType.THINKING), e.getDelta());
            }
        } else if (event instanceof ThinkingBlockEndEvent e) {
            emit(step(thinkingId(e.getReplyId(), e.getBlockId()), AgentTraceType.THINKING,
                    "推理完成", AgentTraceStatus.SUCCESS));
        } else if (event instanceof TextBlockStartEvent e) {
            emit(step(textId(e.getReplyId(), e.getBlockId()), AgentTraceType.STATUS,
                    "正在撰写回复", AgentTraceStatus.RUNNING));
        } else if (event instanceof TextBlockEndEvent e) {
            emit(step(textId(e.getReplyId(), e.getBlockId()), AgentTraceType.STATUS,
                    "回复撰写完成", AgentTraceStatus.SUCCESS));
        } else if (event instanceof ToolCallStartEvent e) {
            emit(toolStep(toolId(e.getToolCallId()), AgentTraceType.TOOL_CALL,
                    "准备调用：" + toolLabel(e.getToolCallName()), AgentTraceStatus.RUNNING,
                    e.getToolCallName(), e.getToolCallId()));
        } else if (event instanceof ToolCallDeltaEvent e) {
            if (StringUtils.hasText(e.getDelta())) {
                emitDelta(toolDeltaStep(toolId(e.getToolCallId()), AgentTraceType.TOOL_CALL,
                        e.getToolCallName(), e.getToolCallId()), e.getDelta());
            }
        } else if (event instanceof ToolCallEndEvent e) {
            emit(toolStep(toolId(e.getToolCallId()), AgentTraceType.TOOL_CALL,
                    "参数已就绪", AgentTraceStatus.RUNNING,
                    e.getToolCallName(), e.getToolCallId()));
        } else if (event instanceof ToolResultStartEvent e) {
            emit(toolStep(toolId(e.getToolCallId()), AgentTraceType.TOOL_RESULT,
                    "执行：" + toolLabel(e.getToolCallName()), AgentTraceStatus.RUNNING,
                    e.getToolCallName(), e.getToolCallId()));
        } else if (event instanceof ToolResultTextDeltaEvent e) {
            if (StringUtils.hasText(e.getDelta())) {
                emitDelta(toolDeltaStep(toolId(e.getToolCallId()), AgentTraceType.TOOL_RESULT,
                        e.getToolCallName(), e.getToolCallId()), e.getDelta());
            }
        } else if (event instanceof ToolResultEndEvent e) {
            AgentTraceStatus status = resultStatus(e.getState());
            emit(toolStep(toolId(e.getToolCallId()), AgentTraceType.TOOL_RESULT,
                    resultTitle(status), status, e.getToolCallName(), e.getToolCallId()));
        } else if (event instanceof RequireUserConfirmEvent e) {
            emitConfirmSteps(e);
        } else if (event instanceof AgentResultEvent) {
            emit(step(TRACE_STATUS_RESULT, AgentTraceType.STATUS,
                    "回复已生成", AgentTraceStatus.SUCCESS));
        } else if (event instanceof ExceedMaxItersEvent e) {
            emit(stepWithDetail(TRACE_STATUS_MAX_ITERS, AgentTraceType.STATUS,
                    "达到迭代上限", AgentTraceStatus.ERROR,
                    e.getCurrentIter() + "/" + e.getMaxIters()));
        } else if (event instanceof AllToolsDeniedEvent e) {
            emitDeniedSteps(e.getDeniedToolCalls());
        } else if (event instanceof RequestStopEvent e) {
            emit(stepWithDetail(TRACE_STATUS_REQUEST_STOP, AgentTraceType.STATUS,
                    "请求停止", AgentTraceStatus.ERROR,
                    StringUtils.hasText(e.getReason()) ? e.getReason() : null));
        }
        // AgentEndEvent、TextBlockDeltaEvent、Data/Hint/Custom/Subagent 等事件不展示。
    }

    private void emitConfirmSteps(RequireUserConfirmEvent event) {
        List<ToolUseBlock> toolCalls = event.getToolCalls();
        if (toolCalls == null || toolCalls.isEmpty()) {
            return;
        }
        for (ToolUseBlock toolCall : toolCalls) {
            String detail = toolLabel(toolCall.getName());
            if (toolCall.getInput() != null) {
                detail = detail + " " + toolCall.getInput();
            }
            emit(new AgentTraceStep(
                    toolId(toolCall.getId()), AgentTraceType.TOOL_CALL,
                    "等待确认：" + toolLabel(toolCall.getName()), AgentTraceStatus.PENDING,
                    detail, null, toolCall.getName(), toolCall.getId(), now(), null));
        }
        String names = toolCalls.stream()
                .map(ToolUseBlock::getName)
                .filter(StringUtils::hasText)
                .map(this::toolLabel)
                .distinct()
                .reduce((left, right) -> left + "、" + right)
                .orElse("工具");
        emit(stepWithDetail(confirmId(event.getReplyId()), AgentTraceType.STATUS,
                "等待确认工具调用", AgentTraceStatus.PENDING, names));
    }

    private void emitDeniedSteps(List<ToolUseBlock> deniedToolCalls) {
        if (deniedToolCalls != null) {
            for (ToolUseBlock toolCall : deniedToolCalls) {
                emit(toolStep(toolId(toolCall.getId()), AgentTraceType.TOOL_CALL,
                        "工具被拒绝：" + toolLabel(toolCall.getName()), AgentTraceStatus.DENIED,
                        toolCall.getName(), toolCall.getId()));
            }
        }
        emit(step(TRACE_STATUS_TOOLS_DENIED, AgentTraceType.STATUS,
                "工具被拒绝", AgentTraceStatus.DENIED));
    }

    private AgentTraceStatus resultStatus(ToolResultState state) {
        if (state == null || state == ToolResultState.RUNNING) {
            return AgentTraceStatus.RUNNING;
        }
        return switch (state) {
            case SUCCESS -> AgentTraceStatus.SUCCESS;
            case DENIED -> AgentTraceStatus.DENIED;
            default -> AgentTraceStatus.ERROR;
        };
    }

    private String resultTitle(AgentTraceStatus status) {
        return switch (status) {
            case SUCCESS -> "工具完成";
            case DENIED -> "工具已拒绝";
            case ERROR -> "工具失败";
            default -> "工具执行中";
        };
    }

    private String usageText(ChatUsage usage) {
        if (usage == null) {
            return null;
        }
        return "tokens 输入 " + usage.getInputTokens() + " / 输出 " + usage.getOutputTokens();
    }

    private AgentTraceStep step(String id, AgentTraceType type,
                                String title, AgentTraceStatus status) {
        return new AgentTraceStep(id, type, title, status, null, null,
                null, null, now(), null);
    }

    private AgentTraceStep stepWithDetail(String id, AgentTraceType type, String title,
                                          AgentTraceStatus status, String detail) {
        return new AgentTraceStep(id, type, title, status, detail, null,
                null, null, now(), null);
    }

    private AgentTraceStep toolStep(String id, AgentTraceType type, String title,
                                    AgentTraceStatus status, String toolName, String toolCallId) {
        return new AgentTraceStep(id, type, title, status, null, null,
                toolName, toolCallId, now(), null);
    }

    private AgentTraceStep deltaStep(String id, AgentTraceType type) {
        return new AgentTraceStep(id, type, null, AgentTraceStatus.RUNNING,
                null, null, null, null, null, null);
    }

    private AgentTraceStep toolDeltaStep(String id, AgentTraceType type,
                                         String toolName, String toolCallId) {
        return new AgentTraceStep(id, type, null, AgentTraceStatus.RUNNING,
                null, null, toolName, toolCallId, null, null);
    }

    private void emit(AgentTraceStep step) {
        listener.onProgress(new AgentProgressEvent(step, null));
    }

    private void emitDelta(AgentTraceStep step, String delta) {
        listener.onProgress(new AgentProgressEvent(step, delta));
    }

    private String modelId(String replyId) {
        return "status:model:" + safe(replyId);
    }

    private String textId(String replyId, String blockId) {
        return "status:text:" + safe(replyId) + ":" + safe(blockId);
    }

    private String thinkingId(String replyId, String blockId) {
        return "thinking:" + safe(replyId) + ":" + safe(blockId);
    }

    private String confirmId(String replyId) {
        return "status:confirm:" + safe(replyId);
    }

    private String toolId(String toolCallId) {
        return "tool:" + safe(toolCallId);
    }

    private String safe(String value) {
        return StringUtils.hasText(value) ? value : "unknown";
    }

    private String toolLabel(String toolName) {
        if (!StringUtils.hasText(toolName)) {
            return "工具";
        }
        return switch (toolName) {
            case "proposeVideoPrompt" -> "规划视频提示词";
            case "generateStoryboard" -> "生成分镜脚本";
            case "generateVideoClip" -> "创建视频任务";
            default -> "调用工具 " + toolName;
        };
    }

    private String now() {
        return Instant.now().toString();
    }
}
