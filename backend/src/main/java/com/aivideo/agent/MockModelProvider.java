package com.aivideo.agent;

import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Mock 模型实现：不发起任何网络请求，返回固定格式的模拟回复。
 *
 * <p>真实 AgentScope 模式由 {@link AgentScopeChatAgent} 提供。</p>
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
            if ("generateVideoClip".equals(toolName)) {
                PromptProposal proposal = new PromptProposal(
                        content,
                        content + "。补充明确的镜头运动、主体动作、环境光线和电影感画面细节。",
                        List.of(
                                "理解用户的主体、场景和画面目标",
                                "补充运镜、光线、动作和视觉风格",
                                "保留用户原意并形成可执行提示词"),
                        "proposeVideoPrompt");
                return new AgentReply(
                        "（Mock 模式）我已整理出一版更完整的视频提示词，请选择原版或优化版后确认生成。",
                        false,
                        "proposeVideoPrompt",
                        null,
                        proposal);
            }
            return new AgentReply(
                    "（Mock 模式）我理解你的创作需求：「" + excerpt + "」。\n\n"
                            + "我会先拆解剧本结构，并调用 " + toolName + " 生成占位分镜/镜头参数。"
                            + ("generateVideoClip".equals(toolName)
                            ? "视频任务将在本轮回复后提交。"
                            : "分镜结果会用于后续视频创作。"),
                    "generateVideoClip".equals(toolName),
                    toolName,
                    null,
                    null);
        }

        return new AgentReply(
                "（Mock 模式）你好，我是 AI 视频创作助手。你可以告诉我视频主题、风格、时长或分镜想法，"
                        + "我会帮你整理成可执行的创作方案。\n\n"
                        + "当前收到：「" + excerpt + "」\n"
                        + "提示：当前为 Mock 对话模式；视频服务按 VIDEO_PROVIDER 配置执行。",
                false,
                null,
                null,
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
