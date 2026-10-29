package com.aivideo.agent;

/**
 * 传给 Agent 的历史消息（与持久化层解耦）。
 */
public record AgentMessage(String role, String content) {
}
