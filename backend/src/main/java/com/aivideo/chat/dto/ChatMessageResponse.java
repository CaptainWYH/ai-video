package com.aivideo.chat.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

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
    private LocalDateTime createdAt;
}
