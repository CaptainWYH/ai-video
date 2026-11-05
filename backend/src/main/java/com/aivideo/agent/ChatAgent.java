package com.aivideo.agent;

/**
 * 对话 Agent 抽象。ChatService 只依赖该接口，不直接耦合具体模型。
 */
public interface ChatAgent {

    AgentOutcome chat(AgentRequest request);

    /**
     * 恢复因工具确认而暂停的 Agent。
     */
    AgentOutcome confirm(AgentConfirmRequest request);

    /**
     * 清理某会话的 Agent 状态，用于落库失败后避免残留 pendding 工具调用。
     */
    default void reset(Long userId, Long sessionId) {
    }
}
