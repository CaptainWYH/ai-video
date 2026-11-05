package com.aivideo.agent;

import java.util.Map;

/**
 * Agent 工具调用摘要。只保留前端展示与恢复执行所需的字段，
 * 不把 AgentScope 的 ToolUseBlock 直接暴露给业务层。
 */
public record AgentToolCall(String id, String name, Map<String, Object> input) {

    public AgentToolCall {
        input = input == null ? Map.of() : input;
    }
}
