package com.aivideo.agent.tools;

import com.aivideo.video.dto.VideoTaskResponse;
import com.aivideo.video.dto.VideoGenerationParams;
import com.aivideo.video.service.VideoGenerationService;
import io.agentscope.core.agent.RuntimeContext;
import io.agentscope.core.message.ToolUseBlock;
import io.agentscope.core.message.ToolResultBlock;
import io.agentscope.core.tool.ToolCallParam;
import io.agentscope.core.tool.Toolkit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VideoAssistantToolsTest {

    @Mock
    private VideoGenerationService videoGenerationService;

    @Test
    void toolCreatesTaskAndReturnsTaskIdToAgent() {
        VideoTaskResponse task = VideoTaskResponse.builder()
                .id(42L)
                .status("PENDING")
                .build();
        when(videoGenerationService.createFromChat(
                eq(7L), eq(9L), isNull(), eq("海边日落"),
                any(VideoGenerationParams.class), any()))
                .thenReturn(task);

        VideoToolExecutionContext context = new VideoToolExecutionContext(
                7L, 9L, new VideoGenerationParams(
                        "cinematic", "16:9", 5, "480p", true, false, "mp4"));
        String result = new VideoAssistantTools(videoGenerationService)
                .generateVideoClip("海边日落", "anime", "9:16", 10, "1080p",
                        false, true, "mov", context);

        assertEquals(42L, context.videoTaskId());
        assertTrue(result.contains("taskId=42"));
        ArgumentCaptor<VideoGenerationParams> paramsCaptor =
                ArgumentCaptor.forClass(VideoGenerationParams.class);
        org.mockito.Mockito.verify(videoGenerationService).createFromChat(
                eq(7L), eq(9L), isNull(), eq("海边日落"), paramsCaptor.capture(), any());
        VideoGenerationParams params = paramsCaptor.getValue();
        assertEquals("cinematic", params.style());
        assertEquals("16:9", params.aspect());
        assertEquals(5, params.duration());
        assertEquals("480p", params.resolution());
        assertEquals(true, params.generateAudio());
        assertEquals(false, params.watermark());
        assertEquals("mp4", params.outputFormat());
    }

    @Test
    void toolkitInjectsCallScopedVideoContext() {
        VideoTaskResponse task = VideoTaskResponse.builder()
                .id(88L)
                .status("PENDING")
                .build();
        when(videoGenerationService.createFromChat(
                eq(7L), eq(9L), isNull(), eq("海浪慢镜头"),
                any(VideoGenerationParams.class), any()))
                .thenReturn(task);

        Toolkit toolkit = new Toolkit();
        toolkit.registerTool(new VideoAssistantTools(videoGenerationService));
        VideoToolExecutionContext context = new VideoToolExecutionContext(
                7L, 9L, VideoGenerationParams.defaults());
        RuntimeContext runtimeContext = RuntimeContext.builder()
                .userId("7")
                .sessionId("9")
                .put(VideoToolExecutionContext.class, context)
                .build();
        ToolCallParam param = ToolCallParam.builder()
                .toolUseBlock(ToolUseBlock.builder()
                        .id("call-1")
                        .name("generateVideoClip")
                        .input(Map.of(
                                "prompt", "海浪慢镜头",
                                "style", "realistic",
                                "aspect", "16:9",
                                "durationSeconds", 5,
                                "resolution", "720p",
                                "generateAudio", true,
                                "watermark", false,
                                "outputFormat", "mp4"))
                        .content("{\"prompt\":\"海浪慢镜头\",\"style\":\"realistic\","
                                + "\"aspect\":\"16:9\",\"durationSeconds\":5,"
                                + "\"resolution\":\"720p\",\"generateAudio\":true,"
                                + "\"watermark\":false,\"outputFormat\":\"mp4\"}")
                        .build())
                .runtimeContext(runtimeContext)
                .build();

        ToolResultBlock result = toolkit.callTool(param).block();

        assertFalse(result == null || result.getState().name().equals("ERROR"));
        assertEquals(88L, context.videoTaskId());
    }

    @Test
    void promptProposalToolStoresReviewablePlanInContext() {
        VideoToolExecutionContext context = new VideoToolExecutionContext(
                7L, 9L, VideoGenerationParams.defaults());

        String result = new VideoAssistantTools(videoGenerationService).proposeVideoPrompt(
                "海边日落",
                "黄金时刻的海边日落，低机位缓慢推进，海浪细节清晰，电影感暖色调",
                java.util.List.of("识别主体与环境", "补充运镜和光线", "确认输出风格"),
                context);

        assertTrue(result.contains("等待用户选择"));
        assertEquals("海边日落", context.promptProposal().originalPrompt());
        assertEquals(3, context.promptProposal().analysisSteps().size());
        assertEquals("proposeVideoPrompt", context.promptProposal().toolName());
    }
}
