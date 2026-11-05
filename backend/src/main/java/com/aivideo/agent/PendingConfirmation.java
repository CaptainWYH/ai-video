package com.aivideo.agent;

import java.time.Instant;
import java.util.List;

/**
 * 进程内挂起的工具确认。重启或多实例切换后失效。
 */
public record PendingConfirmation(String confirmationId,
                                  Long userId,
                                  Long sessionId,
                                  String replyId,
                                  List<AgentToolCall> toolCalls,
                                  Long messageId,
                                  Instant expiresAt) {

    public PendingConfirmation {
        toolCalls = toolCalls == null ? List.of() : List.copyOf(toolCalls);
    }

    public boolean expired() {
        return Instant.now().isAfter(expiresAt);
    }
}
