package com.aivideo.agent.tools;

import io.agentscope.core.tool.Tool;
import io.agentscope.core.tool.ToolParam;

/**
 * 视频创作助手的占位工具集，注册到 AgentScope Toolkit。
 *
 * <p>本阶段所有工具仅返回模拟文本，不产生真实视频文件。</p>
 */
public class VideoAssistantTools {

    @Tool(name = "generateStoryboard",
            description = "根据用户的创意描述生成分镜脚本（当前为占位实现）",
            readOnly = true)
    public String generateStoryboard(
            @ToolParam(name = "prompt", description = "用户的创意/剧情描述") String prompt) {
        // TODO 接入真实分镜生成：返回结构化的镜头列表、景别、台词、时长
        return "[MOCK] 已生成占位分镜。原始需求：" + prompt
                + "。镜头 1：开场建立环境；镜头 2：主体动作；镜头 3：结尾收束。";
    }

    @Tool(name = "generateVideoClip",
            description = "根据提示词生成视频片段（当前为占位实现）",
            readOnly = true)
    public String generateVideoClip(
            @ToolParam(name = "prompt", description = "视频画面提示词") String prompt,
            @ToolParam(name = "durationSeconds", description = "视频时长（秒）") Integer durationSeconds) {
        // TODO 接入真实视频生成任务：提交异步任务、轮询进度、写入对象存储
        int duration = durationSeconds == null ? 5 : durationSeconds;
        return "[MOCK] 视频生成能力开发中。prompt=" + prompt + ", duration=" + duration + "s";
    }
}
