package com.aivideo.chat.dto;

import com.aivideo.agent.AgentToolCall;
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
    /** 是否触发视频生成意图，前端据此展示占位卡片 */
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
    private LocalDateTime createdAt;
}
