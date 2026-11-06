package com.aivideo.chat.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class PromptDecisionRequest {

    /** true 使用优化版提示词，false 保留原始提示词 */
    @NotNull(message = "useOptimizedPrompt 不能为空")
    private Boolean useOptimizedPrompt;
}
