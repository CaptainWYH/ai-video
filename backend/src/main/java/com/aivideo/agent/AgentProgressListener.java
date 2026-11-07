package com.aivideo.agent;

/**
 * 推理进度监听器。Mock 实现忽略；真实 Agent 在 streamEvents 的 doOnNext 中逐事件回调。
 */
@FunctionalInterface
public interface AgentProgressListener {

    AgentProgressListener NOOP = event -> {
    };

    void onProgress(AgentProgressEvent event);
}
