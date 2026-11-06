package com.aivideo.agent;

import com.aivideo.video.dto.VideoGenerationParams;

import java.util.List;

/**
 * Agent 调用入参。
 */
public record AgentRequest(Long userId, Long sessionId, String content,
                           List<AgentMessage> history, VideoGenerationParams params,
                           AgentProgressListener progressListener) {

    public AgentRequest {
        history = history == null ? List.of() : List.copyOf(history);
        params = params == null ? VideoGenerationParams.defaults() : params;
        progressListener = progressListener == null
                ? AgentProgressListener.NOOP : progressListener;
    }
}
