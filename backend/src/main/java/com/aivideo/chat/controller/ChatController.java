package com.aivideo.chat.controller;

import com.aivideo.chat.dto.ChatMessageResponse;
import com.aivideo.chat.dto.ChatSessionResponse;
import com.aivideo.chat.dto.ConfirmToolCallRequest;
import com.aivideo.chat.dto.CreateSessionRequest;
import com.aivideo.chat.dto.PromptDecisionRequest;
import com.aivideo.chat.dto.PromptDecisionResponse;
import com.aivideo.chat.dto.SendMessageRequest;
import com.aivideo.chat.dto.ToolConfirmResponse;
import com.aivideo.chat.service.ChatService;
import com.aivideo.common.PageResult;
import com.aivideo.common.Result;
import com.aivideo.security.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "对话", description = "会话管理、历史消息与发送消息")
@RestController
@RequestMapping("/api/v1/chat")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class ChatController {

    private final ChatService chatService;

    @Operation(summary = "创建会话")
    @PostMapping("/sessions")
    public Result<ChatSessionResponse> createSession(@Valid @RequestBody(required = false) CreateSessionRequest request) {
        CreateSessionRequest safeRequest = request == null ? new CreateSessionRequest() : request;
        return Result.ok(chatService.createSession(SecurityUtils.currentUserId(), safeRequest));
    }

    @Operation(summary = "会话列表（分页）")
    @GetMapping("/sessions")
    public Result<PageResult<ChatSessionResponse>> listSessions(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "20") long size) {
        return Result.ok(chatService.listSessions(SecurityUtils.currentUserId(), page, size));
    }

    @Operation(summary = "会话历史消息（分页）")
    @GetMapping("/sessions/{id}/messages")
    public Result<PageResult<ChatMessageResponse>> listMessages(
            @PathVariable("id") Long sessionId,
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "100") long size) {
        return Result.ok(chatService.listMessages(SecurityUtils.currentUserId(), sessionId, page, size));
    }

    @Operation(summary = "发送消息（同步返回，同时通过 WebSocket 推送模拟分片）")
    @PostMapping("/messages")
    public Result<ChatMessageResponse> sendMessage(@Valid @RequestBody SendMessageRequest request) {
        return Result.ok(chatService.sendMessage(SecurityUtils.currentUserId(), request));
    }

    @Operation(summary = "处理工具调用确认并恢复 Agent")
    @PostMapping("/messages/{id}/confirm")
    public Result<ToolConfirmResponse> confirmToolCall(
            @PathVariable("id") Long messageId,
            @Valid @RequestBody ConfirmToolCallRequest request) {
        return Result.ok(chatService.confirmToolCall(
                SecurityUtils.currentUserId(), messageId, request));
    }

    @Operation(summary = "选择原版或优化版提示词并确认生成")
    @PostMapping("/messages/{id}/prompt-decision")
    public Result<PromptDecisionResponse> decidePrompt(
            @PathVariable("id") Long messageId,
            @Valid @RequestBody PromptDecisionRequest request) {
        return Result.ok(chatService.decidePrompt(
                SecurityUtils.currentUserId(), messageId, request));
    }
}
