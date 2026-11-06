package com.aivideo.chat.dto;

import com.aivideo.video.dto.VideoGenerationParams;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class SendMessageRequest {

    @NotNull(message = "sessionId 不能为空")
    private Long sessionId;

    @NotBlank(message = "消息内容不能为空")
    @Size(max = 4000, message = "单条消息不能超过 4000 个字符")
    private String content;

    /** 前端生成，用于关联 WebSocket 流式分片；可选 */
    @Size(max = 64, message = "requestId 不能超过 64 个字符")
    private String requestId;

    /** 当前轮次的视频参数；由视频工具或 Mock 兜底路径创建任务时使用 */
    @Valid
    private VideoGenerationParams params;
}
