package com.aivideo.chat.dto;

import com.aivideo.agent.AgentToolCall;
import com.aivideo.agent.AgentTraceStep;
import com.aivideo.agent.PromptProposal;
import com.aivideo.video.dto.VideoTaskResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessageResponse {

    private Long id;
    private Long sessionId;
    private String role;
    private String content;
    private String messageType;
    /** 是否已创建视频任务，前端据此展示视频卡片 */
    private boolean videoIntent;
    /** 触发的占位工具名，例如 generateStoryboard */
    private String toolName;
    /** 工具确认卡片：确认 ID */
    private String confirmationId;
    /** 工具确认卡片：AgentScope replyId */
    private String replyId;
    /** 工具确认卡片：待确认或已确认的工具调用 */
    private List<AgentToolCall> toolCalls;
    /** 工具确认卡片状态：PENDING / APPROVED / DENIED / EXPIRED / FAILED */
    private String confirmStatus;
    /** Agent 生成的视频提示词优化方案 */
    private PromptProposal promptProposal;
    /** 提示词方案状态：PENDING / SELECTED */
    private String promptDecisionStatus;
    /** 用户最终是否选择了优化版 */
    private Boolean useOptimizedPrompt;
    /** 与 VIDEO_CARD 消息关联的持久化视频任务 */
    private VideoTaskResponse videoTask;
    /** 真实 Agent 的推理过程快照；Mock 模式或旧消息为空 */
    private List<AgentTraceStep> reasoningSteps;
    private LocalDateTime createdAt;
}
