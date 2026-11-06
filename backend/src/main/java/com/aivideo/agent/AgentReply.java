package com.aivideo.agent;

/**
 * Agent 回复结果。
 *
 * @param content     回复文本
 * @param videoIntent 是否命中视频生成意图
 * @param toolName    命中的工具名，可为 null
 * @param videoTaskId 工具已直接创建的视频任务 ID，可为 null
 */
public record AgentReply(String content,
                         boolean videoIntent,
                         String toolName,
                         Long videoTaskId,
                         PromptProposal promptProposal) {
}
