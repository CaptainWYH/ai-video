package com.aivideo.agent.tools;

import com.aivideo.agent.PromptProposal;
import com.aivideo.video.dto.VideoGenerationParams;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 单次 Agent 调用的视频工具上下文，由 RuntimeContext 注入工具方法。
 */
public final class VideoToolExecutionContext {

    private final Long userId;
    private final Long sessionId;
    private final VideoGenerationParams params;
    private final String idempotencyKey;
    private final AtomicReference<Long> videoTaskId = new AtomicReference<>();
    private final AtomicReference<PromptProposal> promptProposal = new AtomicReference<>();

    public VideoToolExecutionContext(Long userId, Long sessionId, VideoGenerationParams params) {
        this(userId, sessionId, params, UUID.randomUUID().toString());
    }

    public VideoToolExecutionContext(Long userId, Long sessionId,
                                     VideoGenerationParams params,
                                     String idempotencyKey) {
        this.userId = userId;
        this.sessionId = sessionId;
        this.params = params == null ? VideoGenerationParams.defaults() : params;
        this.idempotencyKey = idempotencyKey;
    }

    public Long userId() {
        return userId;
    }

    public Long sessionId() {
        return sessionId;
    }

    public VideoGenerationParams params() {
        return params;
    }

    public String idempotencyKey() {
        return idempotencyKey;
    }

    public void taskCreated(Long taskId) {
        videoTaskId.set(taskId);
    }

    public Long videoTaskId() {
        return videoTaskId.get();
    }

    public void promptProposed(PromptProposal proposal) {
        promptProposal.set(proposal);
    }

    public PromptProposal promptProposal() {
        return promptProposal.get();
    }
}
