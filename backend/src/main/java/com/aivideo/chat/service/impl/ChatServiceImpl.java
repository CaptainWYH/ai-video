package com.aivideo.chat.service.impl;

import com.aivideo.agent.AgentConfirmRequest;
import com.aivideo.agent.AgentFactory;
import com.aivideo.agent.AgentMessage;
import com.aivideo.agent.AgentOutcome;
import com.aivideo.agent.AgentReply;
import com.aivideo.agent.AgentRequest;
import com.aivideo.agent.AgentScopeProperties;
import com.aivideo.agent.AgentToolCall;
import com.aivideo.agent.ModelErrorTranslator;
import com.aivideo.agent.PendingConfirmation;
import com.aivideo.agent.PendingConfirmationRegistry;
import com.aivideo.agent.PromptProposal;
import com.aivideo.agent.tools.VideoAssistantTools;
import com.aivideo.agent.tools.VideoToolExecutionContext;
import com.aivideo.chat.dto.ChatMessageResponse;
import com.aivideo.chat.dto.ChatSessionResponse;
import com.aivideo.chat.dto.ConfirmToolCallRequest;
import com.aivideo.chat.dto.CreateSessionRequest;
import com.aivideo.chat.dto.PromptDecisionRequest;
import com.aivideo.chat.dto.PromptDecisionResponse;
import com.aivideo.chat.dto.SendMessageRequest;
import com.aivideo.chat.dto.ToolConfirmResponse;
import com.aivideo.chat.entity.ChatMessage;
import com.aivideo.chat.entity.ChatSession;
import com.aivideo.chat.mapper.ChatMessageMapper;
import com.aivideo.chat.mapper.ChatSessionMapper;
import com.aivideo.chat.service.ChatService;
import com.aivideo.common.BizException;
import com.aivideo.common.PageResult;
import com.aivideo.video.dto.VideoTaskResponse;
import com.aivideo.video.dto.VideoGenerationParams;
import com.aivideo.video.service.VideoGenerationService;
import com.aivideo.websocket.ChatPushService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatServiceImpl implements ChatService {

    private static final String DEFAULT_SESSION_TITLE = "新会话";
    private static final int MAX_PAGE_SIZE = 100;
    private static final String MESSAGE_TYPE_TOOL_CONFIRM = "TOOL_CONFIRM";
    private static final String CONFIRM_STATUS_PENDING = "PENDING";
    private static final String CONFIRM_STATUS_APPROVED = "APPROVED";
    private static final String CONFIRM_STATUS_DENIED = "DENIED";
    private static final String CONFIRM_STATUS_EXPIRED = "EXPIRED";
    private static final String CONFIRM_STATUS_FAILED = "FAILED";

    private final ChatSessionMapper chatSessionMapper;
    private final ChatMessageMapper chatMessageMapper;
    private final AgentFactory agentFactory;
    private final AgentScopeProperties agentScopeProperties;
    private final PendingConfirmationRegistry pendingConfirmationRegistry;
    private final ChatPushService chatPushService;
    private final ObjectMapper objectMapper;
    private final TransactionTemplate transactionTemplate;
    private final VideoGenerationService videoGenerationService;
    private final VideoAssistantTools videoAssistantTools;

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
        List<Long> messageIds = result.getRecords().stream().map(ChatMessage::getId).toList();
        Map<Long, VideoTaskResponse> videoTasks = videoGenerationService
                .findTasksForChatMessages(userId, messageIds);
        List<ChatMessageResponse> records = result.getRecords().stream()
                .map(message -> toResponse(message, videoTasks.get(message.getId())))
                .toList();
        return PageResult.of(result.getCurrent(), result.getSize(), result.getTotal(), records);
    }

    @Override
    public ChatMessageResponse sendMessage(Long userId, SendMessageRequest request) {
        ChatSession session = getOwnedSession(userId, request.getSessionId());
        if (pendingConfirmationRegistry.hasPending(userId, session.getId())) {
            throw BizException.conflict("请先处理当前工具确认");
        }

        List<AgentMessage> history = loadHistory(session.getId());

        // 模型调用在事务外，避免真实网络请求长时间占用数据库连接。
        AgentOutcome outcome = agentFactory.getAgent()
                .chat(new AgentRequest(userId, session.getId(), request.getContent(),
                        history, request.getParams()));

        if (outcome instanceof AgentOutcome.AwaitingConfirmation awaiting) {
            return saveAwaitingConfirmation(userId, session, request, awaiting);
        }

        AgentReply reply = ((AgentOutcome.Completed) outcome).reply();
        ChatMessageResponse response;
        try {
            response = Objects.requireNonNull(transactionTemplate.execute(status -> {
                insertUserMessage(userId, session.getId(), request.getContent());
                ChatMessageResponse assistant = insertAssistantMessage(
                        userId, session.getId(), reply, request.getContent(), request.getParams());
                touchSession(session, request.getContent());
                return assistant;
            }));
        } catch (RuntimeException ex) {
            // 落库失败时清理 AgentState，避免残留 pending 工具调用。
            agentFactory.getAgent().reset(userId, session.getId());
            throw ex;
        }

        String requestId = resolveRequestId(request.getRequestId());
        chatPushService.streamAssistantMessage(userId, session.getId(), requestId, response);
        return response;
    }

    @Override
    public ToolConfirmResponse confirmToolCall(Long userId, Long messageId,
                                               ConfirmToolCallRequest request) {
        ChatMessage confirmMessage = getOwnedMessage(userId, messageId);
        if (!MESSAGE_TYPE_TOOL_CONFIRM.equals(confirmMessage.getMessageType())) {
            throw BizException.badRequest("该消息不是工具确认消息");
        }

        ConfirmMetadata metadata = parseConfirmMetadata(confirmMessage.getMetadata());
        if (!CONFIRM_STATUS_PENDING.equals(metadata.confirmStatus())) {
            throw BizException.conflict("该工具确认已处理");
        }

        String requestId = resolveRequestId(request.getRequestId());
        PendingConfirmation pending = pendingConfirmationRegistry.claim(
                metadata.confirmationId(), userId, messageId);
        if (pending == null) {
            updateConfirmStatus(confirmMessage, CONFIRM_STATUS_EXPIRED);
            chatPushService.pushToolConfirmResult(userId, confirmMessage.getSessionId(), requestId,
                    confirmMessage.getId(), metadata.confirmationId(), metadata.replyId(),
                    CONFIRM_STATUS_EXPIRED, metadata.toolCalls());
            throw BizException.conflict("工具确认已失效，请重新发送消息");
        }

        boolean approved = Boolean.TRUE.equals(request.getApproved());
        String resultStatus = approved ? CONFIRM_STATUS_APPROVED : CONFIRM_STATUS_DENIED;

        AgentOutcome outcome;
        try {
            outcome = agentFactory.getAgent().confirm(new AgentConfirmRequest(
                    userId, confirmMessage.getSessionId(), pending.replyId(),
                    pending.toolCalls(), approved, pending.params()));
        } catch (BizException ex) {
            markConfirmationFailed(userId, confirmMessage, pending, requestId);
            throw ex;
        } catch (RuntimeException ex) {
            markConfirmationFailed(userId, confirmMessage, pending, requestId);
            throw ModelErrorTranslator.translate(ex);
        }

        if (outcome instanceof AgentOutcome.AwaitingConfirmation awaiting) {
            return saveFollowUpConfirmation(userId, confirmMessage, pending, awaiting,
                    resultStatus, requestId);
        }

        AgentReply reply = ((AgentOutcome.Completed) outcome).reply();
        // 恢复调用已成功，此时才落库；落库失败按 500 暴露，不要再翻译成模型错误。
        ToolConfirmResponse response = Objects.requireNonNull(transactionTemplate.execute(status -> {
            ChatMessageResponse updated = updateConfirmStatus(confirmMessage, resultStatus);
            ChatMessageResponse replyResponse = insertAssistantMessage(
                    userId, confirmMessage.getSessionId(), reply,
                    videoPromptOf(pending.toolCalls(), "生成一个视频短片"), null);
            touchSession(confirmMessage.getSessionId());
            return ToolConfirmResponse.builder()
                    .confirmMessage(updated)
                    .replyMessage(replyResponse)
                    .build();
        }));

        chatPushService.pushToolConfirmResult(userId, confirmMessage.getSessionId(), requestId,
                confirmMessage.getId(), pending.confirmationId(), pending.replyId(),
                resultStatus, pending.toolCalls());
        chatPushService.streamAssistantMessage(userId, confirmMessage.getSessionId(),
                requestId, response.getReplyMessage());
        return response;
    }

    @Override
    public PromptDecisionResponse decidePrompt(Long userId, Long messageId,
                                               PromptDecisionRequest request) {
        ChatMessage proposalMessage = getOwnedMessage(userId, messageId);
        if (!"PROMPT_PROPOSAL".equals(proposalMessage.getMessageType())) {
            throw BizException.badRequest("该消息不是提示词优化方案");
        }
        PromptMetadata promptMetadata = parsePromptMetadata(proposalMessage.getMetadata());
        if (!CONFIRM_STATUS_PENDING.equals(promptMetadata.status())) {
            throw BizException.conflict("该提示词方案已处理");
        }

        boolean useOptimized = Boolean.TRUE.equals(request.getUseOptimizedPrompt());
        String selectedPrompt = useOptimized
                ? promptMetadata.proposal().optimizedPrompt()
                : promptMetadata.proposal().originalPrompt();
        VideoToolExecutionContext toolContext = new VideoToolExecutionContext(
                userId,
                proposalMessage.getSessionId(),
                promptMetadata.params(),
                "prompt:" + proposalMessage.getId());

        PromptDecisionResponse response = Objects.requireNonNull(
                transactionTemplate.execute(status -> {
                    videoAssistantTools.generateVideoClip(
                            selectedPrompt, null, null, null, null,
                            null, null, null, toolContext);
                    Long videoTaskId = toolContext.videoTaskId();
                    if (videoTaskId == null) {
                        throw new IllegalStateException("视频工具未返回任务 ID");
                    }
                    ChatMessageResponse updatedProposal =
                            updatePromptDecision(proposalMessage, useOptimized, selectedPrompt);
                    ChatMessageResponse videoMessage = insertAssistantMessage(
                            userId,
                            proposalMessage.getSessionId(),
                            new AgentReply(
                                    "已按确认后的提示词创建视频生成任务。",
                                    true,
                                    "generateVideoClip",
                                    videoTaskId,
                                    null),
                            selectedPrompt,
                            promptMetadata.params());
                    touchSession(proposalMessage.getSessionId());
                    return PromptDecisionResponse.builder()
                            .proposalMessage(updatedProposal)
                            .videoMessage(videoMessage)
                            .build();
                }));
        return response;
    }

    private ChatMessageResponse saveAwaitingConfirmation(Long userId,
                                                         ChatSession session,
                                                         SendMessageRequest request,
                                                         AgentOutcome.AwaitingConfirmation awaiting) {
        String confirmationId = UUID.randomUUID().toString();
        ChatMessageResponse response;
        try {
            response = Objects.requireNonNull(transactionTemplate.execute(status -> {
                insertUserMessage(userId, session.getId(), request.getContent());

                ChatMessage confirmMessage = new ChatMessage();
                confirmMessage.setSessionId(session.getId());
                confirmMessage.setUserId(userId);
                confirmMessage.setRole(ChatMessage.ROLE_ASSISTANT);
                confirmMessage.setContent(StringUtils.hasText(awaiting.content())
                        ? awaiting.content()
                        : "需要你确认工具调用：" + toolNameOf(awaiting.toolCalls()));
                confirmMessage.setMessageType(MESSAGE_TYPE_TOOL_CONFIRM);
                confirmMessage.setMetadata(buildConfirmMetadata(
                        awaiting, confirmationId, CONFIRM_STATUS_PENDING));
                chatMessageMapper.insert(confirmMessage);

                touchSession(session, request.getContent());
                return toResponse(confirmMessage);
            }));
        } catch (RuntimeException ex) {
            agentFactory.getAgent().reset(userId, session.getId());
            throw ex;
        }

        pendingConfirmationRegistry.register(new PendingConfirmation(
                confirmationId, userId, session.getId(), awaiting.replyId(),
                awaiting.toolCalls(), request.getParams(), response.getId(),
                pendingConfirmationRegistry.expiresAt()));

        String requestId = resolveRequestId(request.getRequestId());
        chatPushService.pushToolConfirmRequired(userId, session.getId(), requestId, response);
        return response;
    }

    private ToolConfirmResponse saveFollowUpConfirmation(Long userId,
                                                         ChatMessage confirmMessage,
                                                         PendingConfirmation pending,
                                                         AgentOutcome.AwaitingConfirmation awaiting,
                                                         String resultStatus,
                                                         String requestId) {
        String newConfirmationId = UUID.randomUUID().toString();
        ToolConfirmResponse response;
        try {
            response = Objects.requireNonNull(transactionTemplate.execute(status -> {
                ChatMessageResponse updated = updateConfirmStatus(confirmMessage, resultStatus);

                ChatMessage newCard = new ChatMessage();
                newCard.setSessionId(confirmMessage.getSessionId());
                newCard.setUserId(userId);
                newCard.setRole(ChatMessage.ROLE_ASSISTANT);
                newCard.setContent(StringUtils.hasText(awaiting.content())
                        ? awaiting.content()
                        : "需要你确认工具调用：" + toolNameOf(awaiting.toolCalls()));
                newCard.setMessageType(MESSAGE_TYPE_TOOL_CONFIRM);
                newCard.setMetadata(buildConfirmMetadata(
                        awaiting, newConfirmationId, CONFIRM_STATUS_PENDING));
                chatMessageMapper.insert(newCard);

                touchSession(confirmMessage.getSessionId());
                return ToolConfirmResponse.builder()
                        .confirmMessage(updated)
                        .replyMessage(toResponse(newCard))
                        .build();
            }));
        } catch (RuntimeException ex) {
            // 旧卡片已被 claim，落库失败后标记为 FAILED，避免前端停留在 PENDING。
            markConfirmationFailed(userId, confirmMessage, pending, requestId);
            throw ex;
        }

        pendingConfirmationRegistry.register(new PendingConfirmation(
                newConfirmationId, userId, confirmMessage.getSessionId(), awaiting.replyId(),
                awaiting.toolCalls(), pending.params(), response.getReplyMessage().getId(),
                pendingConfirmationRegistry.expiresAt()));

        chatPushService.pushToolConfirmResult(userId, confirmMessage.getSessionId(), requestId,
                confirmMessage.getId(), pending.confirmationId(), pending.replyId(),
                resultStatus, pending.toolCalls());
        chatPushService.pushToolConfirmRequired(userId, confirmMessage.getSessionId(), requestId,
                response.getReplyMessage());
        return response;
    }

    private void markConfirmationFailed(Long userId,
                                        ChatMessage confirmMessage,
                                        PendingConfirmation pending,
                                        String requestId) {
        try {
            updateConfirmStatus(confirmMessage, CONFIRM_STATUS_FAILED);
            chatPushService.pushToolConfirmResult(userId, confirmMessage.getSessionId(), requestId,
                    confirmMessage.getId(), pending.confirmationId(), pending.replyId(),
                    CONFIRM_STATUS_FAILED, pending.toolCalls());
        } catch (RuntimeException ex) {
            log.warn("标记工具确认失败状态时出错: {}", ex.getMessage());
        }
        agentFactory.getAgent().reset(userId, confirmMessage.getSessionId());
    }

    private ChatSession getOwnedSession(Long userId, Long sessionId) {
        ChatSession session = chatSessionMapper.selectById(sessionId);
        if (session == null || !session.getUserId().equals(userId)) {
            throw BizException.notFound("会话不存在");
        }
        return session;
    }

    private ChatMessage getOwnedMessage(Long userId, Long messageId) {
        ChatMessage message = chatMessageMapper.selectById(messageId);
        if (message == null || !message.getUserId().equals(userId)) {
            throw BizException.notFound("消息不存在");
        }
        return message;
    }

    private List<AgentMessage> loadHistory(Long sessionId) {
        int limit = Math.max(0, Math.min(agentScopeProperties.getMaxHistoryMessages(), 100));
        if (limit == 0) {
            return List.of();
        }
        LambdaQueryWrapper<ChatMessage> wrapper = new LambdaQueryWrapper<ChatMessage>()
                .eq(ChatMessage::getSessionId, sessionId)
                .ne(ChatMessage::getMessageType, MESSAGE_TYPE_TOOL_CONFIRM)
                .orderByDesc(ChatMessage::getId)
                .last("LIMIT " + limit);
        List<ChatMessage> messages = new ArrayList<>(chatMessageMapper.selectList(wrapper));
        Collections.reverse(messages);
        return messages.stream()
                .map(message -> new AgentMessage(message.getRole(), message.getContent()))
                .toList();
    }

    private void insertUserMessage(Long userId, Long sessionId, String content) {
        ChatMessage userMessage = new ChatMessage();
        userMessage.setSessionId(sessionId);
        userMessage.setUserId(userId);
        userMessage.setRole(ChatMessage.ROLE_USER);
        userMessage.setContent(content);
        userMessage.setMessageType("TEXT");
        chatMessageMapper.insert(userMessage);
    }

    private ChatMessageResponse insertAssistantMessage(Long userId, Long sessionId,
                                                       AgentReply reply, String videoPrompt,
                                                       VideoGenerationParams videoParams) {
        ChatMessage assistantMessage = new ChatMessage();
        assistantMessage.setSessionId(sessionId);
        assistantMessage.setUserId(userId);
        assistantMessage.setRole(ChatMessage.ROLE_ASSISTANT);
        assistantMessage.setContent(reply.content());
        assistantMessage.setMetadata(buildMetadata(reply, videoParams));
        assistantMessage.setMessageType(
                reply.promptProposal() != null
                        ? "PROMPT_PROPOSAL"
                        : reply.videoTaskId() != null ? "VIDEO_CARD" : "TEXT");
        chatMessageMapper.insert(assistantMessage);

        VideoTaskResponse videoTask = null;
        if (reply.videoTaskId() != null) {
            videoTask = videoGenerationService.linkTaskToChat(
                    userId, reply.videoTaskId(), sessionId, assistantMessage.getId());
        }
        if (videoTask == null
                && !agentScopeProperties.isEnabled()
                && reply.videoIntent()
                && "generateVideoClip".equals(reply.toolName())) {
            videoTask = videoGenerationService.createFromChat(
                    userId,
                    sessionId,
                    assistantMessage.getId(),
                    videoPrompt,
                    videoParams,
                    "chat:" + assistantMessage.getId());
            assistantMessage.setMessageType("VIDEO_CARD");
        }
        if (videoTask == null) {
            return toResponse(assistantMessage, null);
        }
        String metadata = appendMetadata(assistantMessage.getMetadata(), "videoTaskId", videoTask.getId());
        ChatMessage patch = new ChatMessage();
        patch.setId(assistantMessage.getId());
        patch.setMetadata(metadata);
        patch.setMessageType("VIDEO_CARD");
        chatMessageMapper.updateById(patch);
        assistantMessage.setMetadata(metadata);
        assistantMessage.setMessageType("VIDEO_CARD");
        return toResponse(assistantMessage, videoTask);
    }

    private void touchSession(ChatSession session, String titleSource) {
        session.setLastMessageAt(LocalDateTime.now());
        if (StringUtils.hasText(titleSource) && DEFAULT_SESSION_TITLE.equals(session.getTitle())) {
            session.setTitle(abbreviate(titleSource, 30));
        }
        chatSessionMapper.updateById(session);
    }

    private void touchSession(Long sessionId) {
        ChatSession session = chatSessionMapper.selectById(sessionId);
        if (session == null) {
            return;
        }
        session.setLastMessageAt(LocalDateTime.now());
        chatSessionMapper.updateById(session);
    }

    private ChatMessageResponse updateConfirmStatus(ChatMessage confirmMessage, String status) {
        String metadata = replaceConfirmStatus(confirmMessage.getMetadata(), status);
        ChatMessage patch = new ChatMessage();
        patch.setId(confirmMessage.getId());
        patch.setMetadata(metadata);
        chatMessageMapper.updateById(patch);
        ChatMessage updated = chatMessageMapper.selectById(confirmMessage.getId());
        if (updated == null) {
            // 极端情况下记录被并发删除，用内存中的副本保证响应结构完整。
            confirmMessage.setMetadata(metadata);
            updated = confirmMessage;
        }
        return toResponse(updated);
    }

    private ChatMessageResponse updatePromptDecision(ChatMessage proposalMessage,
                                                     boolean useOptimized,
                                                     String selectedPrompt) {
        try {
            ObjectNode node = StringUtils.hasText(proposalMessage.getMetadata())
                    ? (ObjectNode) objectMapper.readTree(proposalMessage.getMetadata())
                    : objectMapper.createObjectNode();
            node.put("promptDecisionStatus", "SELECTED");
            node.put("useOptimizedPrompt", useOptimized);
            node.put("selectedPrompt", selectedPrompt);
            ChatMessage patch = new ChatMessage();
            patch.setId(proposalMessage.getId());
            patch.setMetadata(objectMapper.writeValueAsString(node));
            chatMessageMapper.updateById(patch);
            ChatMessage updated = chatMessageMapper.selectById(proposalMessage.getId());
            return toResponse(updated == null ? proposalMessage : updated);
        } catch (Exception ex) {
            throw new IllegalStateException("更新提示词选择状态失败", ex);
        }
    }

    private String buildMetadata(AgentReply reply, VideoGenerationParams videoParams) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("videoIntent", reply.videoIntent());
        if (StringUtils.hasText(reply.toolName())) {
            metadata.put("toolName", reply.toolName());
        }
        if (reply.promptProposal() != null) {
            metadata.put("promptProposal", reply.promptProposal());
            metadata.put("promptDecisionStatus", "PENDING");
            metadata.put("videoParams", videoParams == null
                    ? VideoGenerationParams.defaults() : videoParams);
        }
        return writeMetadata(metadata);
    }

    private String buildConfirmMetadata(AgentOutcome.AwaitingConfirmation awaiting,
                                        String confirmationId,
                                        String status) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("videoIntent", false);
        String toolName = toolNameOf(awaiting.toolCalls());
        if (StringUtils.hasText(toolName)) {
            metadata.put("toolName", toolName);
        }
        metadata.put("confirmationId", confirmationId);
        if (StringUtils.hasText(awaiting.replyId())) {
            metadata.put("replyId", awaiting.replyId());
        }
        metadata.put("toolCalls", awaiting.toolCalls());
        metadata.put("confirmStatus", status);
        return writeMetadata(metadata);
    }

    private String writeMetadata(Map<String, Object> metadata) {
        try {
            return objectMapper.writeValueAsString(metadata);
        } catch (Exception ex) {
            log.warn("消息 metadata 序列化失败: {}", ex.getMessage());
            return "{}";
        }
    }

    private String replaceConfirmStatus(String metadata, String status) {
        try {
            ObjectNode node = StringUtils.hasText(metadata)
                    ? (ObjectNode) objectMapper.readTree(metadata)
                    : objectMapper.createObjectNode();
            node.put("confirmStatus", status);
            return objectMapper.writeValueAsString(node);
        } catch (Exception ex) {
            log.warn("确认消息 metadata 更新失败: {}", ex.getMessage());
            return metadata;
        }
    }

    private ConfirmMetadata parseConfirmMetadata(String metadata) {
        if (!StringUtils.hasText(metadata)) {
            return new ConfirmMetadata(null, null, null, List.of());
        }
        try {
            JsonNode node = objectMapper.readTree(metadata);
            List<AgentToolCall> toolCalls = List.of();
            JsonNode toolCallsNode = node.get("toolCalls");
            if (toolCallsNode != null && toolCallsNode.isArray()) {
                toolCalls = objectMapper.convertValue(toolCallsNode,
                        objectMapper.getTypeFactory()
                                .constructCollectionType(List.class, AgentToolCall.class));
            }
            return new ConfirmMetadata(
                    textOf(node, "confirmationId"),
                    textOf(node, "replyId"),
                    textOf(node, "confirmStatus"),
                    toolCalls);
        } catch (Exception ex) {
            log.warn("确认消息 metadata 解析失败: {}", ex.getMessage());
            return new ConfirmMetadata(null, null, null, List.of());
        }
    }

    private PromptMetadata parsePromptMetadata(String metadata) {
        try {
            JsonNode node = objectMapper.readTree(
                    StringUtils.hasText(metadata) ? metadata : "{}");
            JsonNode proposalNode = node.get("promptProposal");
            if (proposalNode == null || proposalNode.isNull()) {
                throw new IllegalArgumentException("缺少提示词优化方案");
            }
            PromptProposal proposal = objectMapper.treeToValue(
                    proposalNode, PromptProposal.class);
            JsonNode paramsNode = node.get("videoParams");
            VideoGenerationParams params = paramsNode == null || paramsNode.isNull()
                    ? VideoGenerationParams.defaults()
                    : objectMapper.treeToValue(paramsNode, VideoGenerationParams.class);
            return new PromptMetadata(
                    proposal,
                    params,
                    textOf(node, "promptDecisionStatus"));
        } catch (Exception ex) {
            throw new IllegalStateException("提示词方案 metadata 解析失败", ex);
        }
    }

    private String textOf(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value == null || value.isNull() ? null : value.asText();
    }

    private String toolNameOf(List<AgentToolCall> toolCalls) {
        if (toolCalls == null || toolCalls.isEmpty()) {
            return null;
        }
        return toolCalls.get(0).name();
    }

    private String resolveRequestId(String requestId) {
        return StringUtils.hasText(requestId) ? requestId : UUID.randomUUID().toString();
    }

    private ChatMessageResponse toResponse(ChatMessage message) {
        return toResponse(message, null);
    }

    private ChatMessageResponse toResponse(ChatMessage message, VideoTaskResponse videoTask) {
        ChatMessageResponse.ChatMessageResponseBuilder builder = ChatMessageResponse.builder()
                .id(message.getId())
                .sessionId(message.getSessionId())
                .role(message.getRole())
                .content(message.getContent())
                .messageType(message.getMessageType())
                .videoTask(videoTask)
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
                String confirmationId = textOf(node, "confirmationId");
                if (StringUtils.hasText(confirmationId)) {
                    builder.confirmationId(confirmationId);
                }
                String replyId = textOf(node, "replyId");
                if (StringUtils.hasText(replyId)) {
                    builder.replyId(replyId);
                }
                String confirmStatus = textOf(node, "confirmStatus");
                if (StringUtils.hasText(confirmStatus)) {
                    builder.confirmStatus(confirmStatus);
                }
                JsonNode toolCallsNode = node.get("toolCalls");
                if (toolCallsNode != null && toolCallsNode.isArray()) {
                    builder.toolCalls(objectMapper.convertValue(toolCallsNode,
                            objectMapper.getTypeFactory()
                                    .constructCollectionType(List.class, AgentToolCall.class)));
                }
                JsonNode proposalNode = node.get("promptProposal");
                if (proposalNode != null && !proposalNode.isNull()) {
                    builder.promptProposal(objectMapper.treeToValue(
                            proposalNode, PromptProposal.class));
                }
                String promptDecisionStatus = textOf(node, "promptDecisionStatus");
                if (StringUtils.hasText(promptDecisionStatus)) {
                    builder.promptDecisionStatus(promptDecisionStatus);
                }
                JsonNode useOptimized = node.get("useOptimizedPrompt");
                if (useOptimized != null && !useOptimized.isNull()) {
                    builder.useOptimizedPrompt(useOptimized.asBoolean());
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

    private String appendMetadata(String metadata, String key, Object value) {
        try {
            ObjectNode node = StringUtils.hasText(metadata)
                    ? (ObjectNode) objectMapper.readTree(metadata)
                    : objectMapper.createObjectNode();
            node.putPOJO(key, value);
            return objectMapper.writeValueAsString(node);
        } catch (Exception ex) {
            throw new IllegalStateException("消息关联视频任务失败", ex);
        }
    }

    private String videoPromptOf(List<AgentToolCall> toolCalls, String fallback) {
        if (toolCalls != null) {
            for (AgentToolCall toolCall : toolCalls) {
                if ("generateVideoClip".equals(toolCall.name())
                        && toolCall.input() != null
                        && toolCall.input().get("prompt") != null) {
                    return toolCall.input().get("prompt").toString();
                }
            }
        }
        return fallback;
    }

    private long normalizePage(long page) {
        return Math.max(1, page);
    }

    private long normalizeSize(long size) {
        return Math.min(MAX_PAGE_SIZE, Math.max(1, size));
    }

    private record ConfirmMetadata(String confirmationId,
                                   String replyId,
                                   String confirmStatus,
                                   List<AgentToolCall> toolCalls) {
    }

    private record PromptMetadata(
            PromptProposal proposal,
            VideoGenerationParams params,
            String status) {
    }
}
