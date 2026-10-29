package com.aivideo.agent;

import java.util.List;

/**
 * Agent 调用入参。
 */
public record AgentRequest(Long userId, Long sessionId, String content, List<AgentMessage> history) {
}
