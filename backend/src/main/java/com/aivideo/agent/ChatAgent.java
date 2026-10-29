package com.aivideo.agent;

/**
 * 对话 Agent 抽象。ChatService 只依赖该接口，不直接耦合具体模型。
 */
public interface ChatAgent {

    AgentReply chat(AgentRequest request);
}
