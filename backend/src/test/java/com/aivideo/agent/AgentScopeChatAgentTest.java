package com.aivideo.agent;

import com.aivideo.agent.tools.VideoToolExecutionContext;
import com.aivideo.video.dto.VideoGenerationParams;
import io.agentscope.core.message.ToolUseBlock;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class AgentScopeChatAgentTest {

    @Test
    void confirmationBlockSerializesInputIntoContent() {
        AgentToolCall call = new AgentToolCall(
                "call-1",
                "generateVideoClip",
                Map.of("prompt", "海边日落", "durationSeconds", 5));

        ToolUseBlock block = AgentScopeChatAgent.toToolUseBlock(call);

        assertNotNull(block.getContent());
        assertFalse(block.getContent().isBlank());
        assertEquals("call-1", block.getId());
        assertEquals("generateVideoClip", block.getName());
        assertEquals(call.input(), block.getInput());
    }

    @Test
    void userInterfaceParametersOverrideModelToolArguments() {
        AgentToolCall modelCall = new AgentToolCall(
                "call-2",
                "generateVideoClip",
                Map.of(
                        "prompt", "海边公路",
                        "style", "anime",
                        "aspect", "9:16",
                        "durationSeconds", 10,
                        "resolution", "1080p",
                        "generateAudio", false,
                        "watermark", true,
                        "outputFormat", "mov"));
        VideoGenerationParams userParams = new VideoGenerationParams(
                "cinematic", "16:9", 5, "480p", true, false, "mp4");

        AgentToolCall aligned = AgentScopeChatAgent.alignVideoToolCall(
                modelCall, new VideoToolExecutionContext(7L, 9L, userParams));

        assertEquals("cinematic", aligned.input().get("style"));
        assertEquals("16:9", aligned.input().get("aspect"));
        assertEquals(5, aligned.input().get("durationSeconds"));
        assertEquals("480p", aligned.input().get("resolution"));
        assertEquals(true, aligned.input().get("generateAudio"));
        assertEquals(false, aligned.input().get("watermark"));
        assertEquals("mp4", aligned.input().get("outputFormat"));
    }
}
