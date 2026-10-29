package com.aivideo.chat.service.impl;

import com.aivideo.agent.AgentFactory;
import com.aivideo.agent.AgentMessage;
import com.aivideo.agent.AgentReply;
import com.aivideo.agent.AgentRequest;
import com.aivideo.agent.AgentScopeProperties;
import com.aivideo.chat.dto.ChatMessageResponse;
import com.aivideo.chat.dto.ChatSessionResponse;
import com.aivideo.chat.dto.CreateSessionRequest;
import com.aivideo.chat.dto.SendMessageRequest;
import com.aivideo.chat.entity.ChatMessage;
import com.aivideo.chat.entity.ChatSession;
import com.aivideo.chat.mapper.ChatMessageMapper;
import com.aivideo.chat.mapper.ChatSessionMapper;
import com.aivideo.chat.service.ChatService;
import com.aivideo.common.BizException;
import com.aivideo.common.PageResult;
import com.aivideo.websocket.ChatPushService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatServiceImpl implements ChatService {

    private static final String DEFAULT_SESSION_TITLE = "新会话";
    private static final int MAX_PAGE_SIZE = 100;

    private final ChatSessionMapper chatSessionMapper;
    private final ChatMessageMapper chatMessageMapper;
    private final AgentFactory agentFactory;
    private final AgentScopeProperties agentScopeProperties;
    private final ChatPushService chatPushService;
    private final ObjectMapper objectMapper;

    @Override
    public ChatSessionResponse createSession(Long userId, CreateSessionRequest request) {
        ChatSession session = new ChatSession();
        session.setUserId(userId);
        session.setTitle(StringUtils.hasText(request.getTitle())
                ? request.getTitle().trim() : DEFAULT_SESSION_TITLE);
        chatSessionMapper.insert(session);
        return ChatSessionResponse.from(session);
    }

    @Override
    public PageResult<ChatSessionResponse> listSessions(Long userId, long page, long size) {
        Page<ChatSession> pageRequest = new Page<>(normalizePage(page), normalizeSize(size));
        LambdaQueryWrapper<ChatSession> wrapper = new LambdaQueryWrapper<ChatSession>()
                .eq(ChatSession::getUserId, userId)
                .orderByDesc(ChatSession::getLastMessageAt)
                .orderByDesc(ChatSession::getId);
        Page<ChatSession> result = chatSessionMapper.selectPage(pageRequest, wrapper);
        List<ChatSessionResponse> records = result.getRecords().stream()
                .map(ChatSessionResponse::from)
                .toList();
        return PageResult.of(result.getCurrent(), result.getSize(), result.getTotal(), records);
    }

    @Override
    public PageResult<ChatMessageResponse> listMessages(Long userId, Long sessionId, long page, long size) {
        getOwnedSession(userId, sessionId);
        Page<ChatMessage> pageRequest = new Page<>(normalizePage(page), normalizeSize(size));
        LambdaQueryWrapper<ChatMessage> wrapper = new LambdaQueryWrapper<ChatMessage>()
                .eq(ChatMessage::getSessionId, sessionId)
                .orderByAsc(ChatMessage::getId);
        Page<ChatMessage> result = chatMessageMapper.selectPage(pageRequest, wrapper);
        List<ChatMessageResponse> records = result.getRecords().stream()
                .map(this::toResponse)
                .toList();
        return PageResult.of(result.getCurrent(), result.getSize(), result.getTotal(), records);
    }

    @Override
    @Transactional
    public ChatMessageResponse sendMessage(Long userId, SendMessageRequest request) {
        ChatSession session = getOwnedSession(userId, request.getSessionId());
        List<AgentMessage> history = loadHistory(session.getId());

        ChatMessage userMessage = new ChatMessage();
        userMessage.setSessionId(session.getId());
        userMessage.setUserId(userId);
        userMessage.setRole(ChatMessage.ROLE_USER);
        userMessage.setContent(request.getContent());
        userMessage.setMessageType("TEXT");
        chatMessageMapper.insert(userMessage);

        AgentReply reply = agentFactory.getAgent()
                .chat(new AgentRequest(userId, session.getId(), request.getContent(), history));

        ChatMessage assistantMessage = new ChatMessage();
        assistantMessage.setSessionId(session.getId());
        assistantMessage.setUserId(userId);
        assistantMessage.setRole(ChatMessage.ROLE_ASSISTANT);
        assistantMessage.setContent(reply.content());
        assistantMessage.setMessageType(reply.videoIntent() ? "VIDEO_CARD" : "TEXT");
        assistantMessage.setMetadata(buildMetadata(reply));
        chatMessageMapper.insert(assistantMessage);

        session.setLastMessageAt(LocalDateTime.now());
        if (DEFAULT_SESSION_TITLE.equals(session.getTitle())) {
            session.setTitle(abbreviate(request.getContent(), 30));
        }
        chatSessionMapper.updateById(session);

        ChatMessageResponse response = toResponse(assistantMessage);
        String requestId = StringUtils.hasText(request.getRequestId())
                ? request.getRequestId() : UUID.randomUUID().toString();
        chatPushService.streamAssistantMessage(userId, session.getId(), requestId, response);
        return response;
    }

    private ChatSession getOwnedSession(Long userId, Long sessionId) {
        ChatSession session = chatSessionMapper.selectById(sessionId);
        if (session == null || !session.getUserId().equals(userId)) {
            throw BizException.notFound("会话不存在");
        }
        return session;
    }

    private List<AgentMessage> loadHistory(Long sessionId) {
        int limit = Math.max(0, Math.min(agentScopeProperties.getMaxHistoryMessages(), 100));
        if (limit == 0) {
            return List.of();
        }
        LambdaQueryWrapper<ChatMessage> wrapper = new LambdaQueryWrapper<ChatMessage>()
                .eq(ChatMessage::getSessionId, sessionId)
                .orderByDesc(ChatMessage::getId)
                .last("LIMIT " + limit);
        List<ChatMessage> messages = new ArrayList<>(chatMessageMapper.selectList(wrapper));
        Collections.reverse(messages);
        return messages.stream()
                .map(message -> new AgentMessage(message.getRole(), message.getContent()))
                .toList();
    }

    private String buildMetadata(AgentReply reply) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("videoIntent", reply.videoIntent());
        if (StringUtils.hasText(reply.toolName())) {
            metadata.put("toolName", reply.toolName());
        }
        try {
            return objectMapper.writeValueAsString(metadata);
        } catch (Exception ex) {
            log.warn("消息 metadata 序列化失败: {}", ex.getMessage());
            return "{}";
        }
    }

    private ChatMessageResponse toResponse(ChatMessage message) {
        ChatMessageResponse.ChatMessageResponseBuilder builder = ChatMessageResponse.builder()
                .id(message.getId())
                .sessionId(message.getSessionId())
                .role(message.getRole())
                .content(message.getContent())
                .messageType(message.getMessageType())
                .createdAt(message.getCreatedAt());

        if (StringUtils.hasText(message.getMetadata())) {
            try {
                JsonNode node = objectMapper.readTree(message.getMetadata());
                JsonNode videoIntent = node.get("videoIntent");
                if (videoIntent != null) {
                    builder.videoIntent(videoIntent.asBoolean(false));
                }
                JsonNode toolName = node.get("toolName");
                if (toolName != null && !toolName.asText().isBlank()) {
                    builder.toolName(toolName.asText());
                }
            } catch (Exception ex) {
                log.warn("消息 metadata 解析失败: {}", ex.getMessage());
            }
        }
        return builder.build();
    }

    private String abbreviate(String content, int maxLength) {
        String normalized = content == null ? "" : content.replaceAll("\\s+", " ").trim();
        if (normalized.length() <= maxLength) {
            return normalized.isEmpty() ? DEFAULT_SESSION_TITLE : normalized;
        }
        return normalized.substring(0, maxLength) + "…";
    }

    private long normalizePage(long page) {
        return Math.max(1, page);
    }

    private long normalizeSize(long size) {
        return Math.min(MAX_PAGE_SIZE, Math.max(1, size));
    }
}
