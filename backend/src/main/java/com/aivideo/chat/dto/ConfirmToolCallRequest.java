package com.aivideo.chat.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "工具调用确认请求")
public class ConfirmToolCallRequest {

    @NotNull(message = "approved 不能为空")
    @Schema(description = "是否允许执行该工具调用", example = "true")
    private Boolean approved;

    @Size(max = 64, message = "requestId 不能超过 64 个字符")
    @Schema(description = "客户端请求 ID，用于关联 WebSocket 事件")
    private String requestId;
}
