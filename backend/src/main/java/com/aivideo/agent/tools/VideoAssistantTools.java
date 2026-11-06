package com.aivideo.agent.tools;

import com.aivideo.agent.PromptProposal;
import com.aivideo.video.dto.VideoTaskResponse;
import com.aivideo.video.dto.VideoGenerationParams;
import com.aivideo.video.service.VideoGenerationService;
import io.agentscope.core.tool.Tool;
import io.agentscope.core.tool.ToolParam;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 视频创作助手工具集，注册到 AgentScope Toolkit。
 *
 * <p>视频工具在用户确认后直接创建异步任务，并通过 RuntimeContext 获取业务上下文。</p>
 */
@Component
@RequiredArgsConstructor
public class VideoAssistantTools {

    private final VideoGenerationService videoGenerationService;

    @Tool(name = "proposeVideoPrompt",
            description = "分析并优化用户的视频提示词，只生成方案，不创建视频任务",
            readOnly = true)
    public String proposeVideoPrompt(
            @ToolParam(name = "originalPrompt", description = "用户原始提示词") String originalPrompt,
            @ToolParam(name = "optimizedPrompt",
                    description = "优化后的完整视频提示词，必须保留用户原意") String optimizedPrompt,
            @ToolParam(name = "analysisSteps",
                    description = "3-5 条可展示的规划步骤，仅描述理解、补全和镜头设计，不输出隐藏思维链")
            List<String> analysisSteps,
            VideoToolExecutionContext context) {
        if (context == null) {
            throw new IllegalStateException("缺少提示词优化上下文");
        }
        PromptProposal proposal = new PromptProposal(
                originalPrompt,
                optimizedPrompt,
                analysisSteps,
                "proposeVideoPrompt");
        context.promptProposed(proposal);
        return "提示词优化方案已生成，等待用户选择原版或优化版后再确认生成视频。";
    }

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
            description = "根据提示词创建异步视频生成任务",
            readOnly = false,
            concurrencySafe = false)
    public String generateVideoClip(
            @ToolParam(name = "prompt", description = "视频画面提示词") String prompt,
            @ToolParam(name = "style", required = false,
                    description = "视觉风格：cinematic、realistic、anime、cyberpunk 或 ink")
            String style,
            @ToolParam(name = "aspect", required = false,
                    description = "画幅：16:9、9:16、1:1、4:3、3:4、21:9 或 adaptive")
            String aspect,
            @ToolParam(name = "durationSeconds", required = false,
                    description = "视频时长，整数秒，范围 4-30")
            Integer durationSeconds,
            @ToolParam(name = "resolution", required = false,
                    description = "分辨率：480p、720p 或 1080p")
            String resolution,
            @ToolParam(name = "generateAudio", required = false,
                    description = "是否生成与画面同步的音频")
            Boolean generateAudio,
            @ToolParam(name = "watermark", required = false,
                    description = "是否生成带水印的视频")
            Boolean watermark,
            @ToolParam(name = "outputFormat", required = false,
                    description = "输出格式：mp4 或 mov")
            String outputFormat,
            VideoToolExecutionContext context) {
        if (context == null) {
            throw new IllegalStateException("缺少视频任务执行上下文");
        }
        if (prompt == null || prompt.isBlank()) {
            throw new IllegalArgumentException("视频提示词不能为空");
        }
        VideoGenerationParams params = context.params().withToolFallbacks(
                style, aspect, durationSeconds, resolution,
                generateAudio, watermark, outputFormat);
        VideoTaskResponse task = videoGenerationService.createFromChat(
                context.userId(),
                context.sessionId(),
                null,
                prompt.trim(),
                params,
                "tool:" + context.idempotencyKey());
        context.taskCreated(task.getId());
        return "视频生成任务已创建：taskId=" + task.getId()
                + "，status=" + task.getStatus()
                + "。任务正在后台生成，请在对话视频卡片中查看进度。";
    }
}
