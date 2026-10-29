package com.aivideo.agent;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Mock 模型实现：不发起任何网络请求，返回固定格式的模拟回复。
 *
 * <p>TODO: 接入真实模型（AgentScope Model / 自有推理服务），保留视频意图识别与工具调用元数据。</p>
 */
@Component
@ConditionalOnProperty(name = "app.agentscope.enabled", havingValue = "false", matchIfMissing = true)
public class MockModelProvider implements ModelProvider {

    @Override
    public AgentReply chat(AgentRequest request) {
        String content = request.content() == null ? "" : request.content().trim();
        String toolName = detectTool(content);
        String excerpt = content.length() > 60 ? content.substring(0, 60) + "…" : content;

        if (toolName != null) {
            return new AgentReply(
                    "（Mock 模式）我理解你的创作需求：「" + excerpt + "」。\n\n"
                            + "我会先拆解剧本结构，并调用 " + toolName + " 生成占位分镜/镜头参数。"
                            + "当前视频生成能力仍在开发中，真实模型与视频渲染接口尚未接入。",
                    true,
                    toolName);
        }

        return new AgentReply(
                "（Mock 模式）你好，我是 AI 视频创作助手。你可以告诉我视频主题、风格、时长或分镜想法，"
                        + "我会帮你整理成可执行的创作方案。\n\n"
                        + "当前收到：「" + excerpt + "」\n"
                        + "提示：本阶段为占位实现，真实模型与视频生成能力尚未接入。",
                false,
                null);
    }

    /**
     * 简单的关键词意图识别，仅用于骨架阶段演示。
     * TODO: 交由真实 Agent 的工具调用结果判断。
     */
    private String detectTool(String content) {
        if (content.contains("分镜") || content.contains("storyboard")) {
            return "generateStoryboard";
        }
        if (content.contains("视频") || content.contains("短片") || content.contains("镜头")) {
            return "generateVideoClip";
        }
        return null;
    }
}
