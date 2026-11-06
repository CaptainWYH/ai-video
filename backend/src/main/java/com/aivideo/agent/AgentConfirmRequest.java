package com.aivideo.agent;

import com.aivideo.video.dto.VideoGenerationParams;

import java.util.List;

/**
 * 用户对挂起工具调用的确认结果。toolCalls 由服务端挂起记录提供，不信任前端。
 */
public record AgentConfirmRequest(Long userId,
                                  Long sessionId,
                                  String replyId,
                                  List<AgentToolCall> toolCalls,
                                  boolean approved,
                                  VideoGenerationParams params) {

    public AgentConfirmRequest {
        toolCalls = toolCalls == null ? List.of() : List.copyOf(toolCalls);
        params = params == null ? VideoGenerationParams.defaults() : params;
    }
}
