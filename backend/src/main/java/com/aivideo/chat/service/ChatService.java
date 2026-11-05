package com.aivideo.chat.service;

import com.aivideo.chat.dto.ChatMessageResponse;
import com.aivideo.chat.dto.ChatSessionResponse;
import com.aivideo.chat.dto.ConfirmToolCallRequest;
import com.aivideo.chat.dto.CreateSessionRequest;
import com.aivideo.chat.dto.SendMessageRequest;
import com.aivideo.chat.dto.ToolConfirmResponse;
import com.aivideo.common.PageResult;

public interface ChatService {

    ChatSessionResponse createSession(Long userId, CreateSessionRequest request);

    PageResult<ChatSessionResponse> listSessions(Long userId, long page, long size);

    PageResult<ChatMessageResponse> listMessages(Long userId, Long sessionId, long page, long size);

    ChatMessageResponse sendMessage(Long userId, SendMessageRequest request);

    /**
     * 处理工具调用确认，并恢复暂停的 Agent。
     */
    ToolConfirmResponse confirmToolCall(Long userId, Long messageId, ConfirmToolCallRequest request);
}
