package com.aivideo.agent;

/**
 * Agent 回复结果。
 *
 * @param content     回复文本
 * @param videoIntent 是否触发视频生成意图（前端展示占位卡片）
 * @param toolName    命中的占位工具名，可为 null
 */
public record AgentReply(String content, boolean videoIntent, String toolName) {
}
