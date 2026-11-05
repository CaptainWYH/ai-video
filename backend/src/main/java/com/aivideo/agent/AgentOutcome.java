package com.aivideo.agent;

import java.util.List;

/**
 * 一次 Agent 调用的结果：要么完成，要么因工具调用等待用户确认。
 */
public sealed interface AgentOutcome
        permits AgentOutcome.Completed, AgentOutcome.AwaitingConfirmation {

    record Completed(AgentReply reply) implements AgentOutcome {
    }

    /**
     * Agent 已暂停，等待用户确认 toolCalls 后再恢复。
     *
     * @param replyId   AgentScope 本次 acting 的 replyId，仅用于追踪
     * @param toolCalls 待确认的工具调用（id/name/input 必须原样保存）
     * @param content   暂停前模型已有的文本，可为空
     */
    record AwaitingConfirmation(String replyId,
                                List<AgentToolCall> toolCalls,
                                String content) implements AgentOutcome {

        public AwaitingConfirmation {
            toolCalls = toolCalls == null ? List.of() : List.copyOf(toolCalls);
            content = content == null ? "" : content;
        }
    }
}
