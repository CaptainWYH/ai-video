package com.aivideo.chat.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "工具调用确认结果")
public class ToolConfirmResponse {

    @Schema(description = "更新后的确认卡片")
    private ChatMessageResponse confirmMessage;

    @Schema(description = "最终助手消息；若 Agent 再次要求确认，则为新的确认卡片")
    private ChatMessageResponse replyMessage;
}
