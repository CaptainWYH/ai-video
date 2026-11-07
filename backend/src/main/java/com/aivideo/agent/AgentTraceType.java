package com.aivideo.agent;

/**
 * 推理轨迹步骤类型。
 */
public enum AgentTraceType {
    STATUS,
    THINKING,
    TOOL_CALL,
    TOOL_RESULT,
    ERROR
}
