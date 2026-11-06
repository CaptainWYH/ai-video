package com.aivideo.agent;

import java.util.List;

/**
 * 可展示给用户的提示词优化方案，不包含模型隐藏思维链。
 */
public record PromptProposal(
        String originalPrompt,
        String optimizedPrompt,
        List<String> analysisSteps,
        String toolName) {

    public PromptProposal {
        analysisSteps = analysisSteps == null ? List.of() : List.copyOf(analysisSteps);
    }
}
