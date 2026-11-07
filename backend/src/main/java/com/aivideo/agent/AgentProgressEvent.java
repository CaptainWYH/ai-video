package com.aivideo.agent;

/**
 * 一次推理进度推送。delta 非空时表示增量文本，前端按 step.type 追加到
 * detail（THINKING）或 result（TOOL_RESULT）；delta 为空时 step 是完整快照。
 */
public record AgentProgressEvent(AgentTraceStep step, String delta) {
}
