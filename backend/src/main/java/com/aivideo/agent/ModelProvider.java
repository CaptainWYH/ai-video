package com.aivideo.agent;

/**
 * 模型/Agent 能力扩展点。
 *
 * <p>后续接入真实大模型时，实现本接口并在配置中替换 {@link MockModelProvider} 即可，
 * 无需修改 ChatService。</p>
 */
public interface ModelProvider {

    AgentReply chat(AgentRequest request);
}
