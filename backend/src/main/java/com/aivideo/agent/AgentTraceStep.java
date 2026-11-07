package com.aivideo.agent;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Agent 单步推理轨迹。持久化在 chat_messages.metadata.reasoningSteps，
 * 也作为 WebSocket chat.progress 的 step 字段返回。
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record AgentTraceStep(
        String id,
        AgentTraceType type,
        String title,
        AgentTraceStatus status,
        String detail,
        String result,
        String toolName,
        String toolCallId,
        String startedAt,
        String endedAt) {
}
