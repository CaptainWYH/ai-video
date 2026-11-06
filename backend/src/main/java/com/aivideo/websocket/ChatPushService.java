package com.aivideo.websocket;

import com.aivideo.agent.AgentProgressEvent;
import com.aivideo.agent.AgentToolCall;
import com.aivideo.chat.dto.ChatMessageResponse;
import com.aivideo.video.dto.VideoUsageResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * WebSocket 推送服务：模拟流式输出与视频任务进度。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChatPushService {

    private static final int CHUNK_SIZE = 6;
    private static final long CHUNK_INTERVAL_MILLIS = 40L;

    private final WebSocketSessionRegistry sessionRegistry;
    private final ObjectMapper objectMapper;

    /**
     * 将完整的助手消息拆成模拟分片推送，最后发送 chat.done。
     */
    @Async("chatPushExecutor")
    public void streamAssistantMessage(Long userId,
                                       Long sessionId,
                                       String requestId,
                                       ChatMessageResponse message) {
        if (sessionRegistry.get(userId).isEmpty()) {
            return;
        }
        String finalRequestId = (requestId == null || requestId.isBlank())
                ? UUID.randomUUID().toString() : requestId;
        String content = message.getContent() == null ? "" : message.getContent();

        for (int i = 0; i < content.length(); i += CHUNK_SIZE) {
            String delta = content.substring(i, Math.min(content.length(), i + CHUNK_SIZE));
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("sessionId", sessionId);
            data.put("requestId", finalRequestId);
            data.put("delta", delta);
            send(userId, "chat.chunk", data);
            sleepQuietly();
        }

        Map<String, Object> done = new LinkedHashMap<>();
        done.put("sessionId", sessionId);
        done.put("requestId", finalRequestId);
        done.put("message", message);
        send(userId, "chat.done", done);
    }

    /**
     * 推送工具调用待确认事件。挂起时不发送 chat.done，前端以本事件结束本轮。
     */
    public void pushToolConfirmRequired(Long userId, Long sessionId,
                                        String requestId, ChatMessageResponse message) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("sessionId", sessionId);
        data.put("requestId", requestId);
        data.put("message", message);
        send(userId, "chat.tool_confirm_required", data);
    }

    /**
     * 推送工具确认结果事件，用于同步确认卡片的最终状态。
     */
    public void pushToolConfirmResult(Long userId, Long sessionId, String requestId,
                                      Long messageId, String confirmationId, String replyId,
                                      String status, List<AgentToolCall> toolCalls) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("sessionId", sessionId);
        data.put("requestId", requestId);
        data.put("messageId", messageId);
        data.put("confirmationId", confirmationId);
        data.put("replyId", replyId);
        data.put("status", status);
        data.put("toolCalls", toolCalls);
        send(userId, "chat.tool_confirm_result", data);
    }

    /**
     * 推送视频任务进度事件。
     */
    public void pushVideoProgress(Long userId,
                                  Long taskId,
                                  Long sessionId,
                                  Long messageId,
                                  String status,
                                  Integer progress,
                                  String resultUrl,
                                  VideoUsageResponse usage) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("taskId", taskId);
        data.put("sessionId", sessionId);
        data.put("messageId", messageId);
        data.put("status", status);
        data.put("progress", progress);
        data.put("resultUrl", resultUrl);
        data.put("usage", usage);
        send(userId, "video.progress", data);
    }

    /**
     * 推送 Agent 推理进度。同步发送以保留同一 requestId 内的事件顺序；
     * 无 WebSocket 连接时直接跳过，最终轨迹以 REST 返回的 reasoningSteps 为准。
     */
    public void pushAgentProgress(Long userId, Long sessionId, String requestId,
                                  AgentProgressEvent event) {
        if (sessionRegistry.get(userId).isEmpty()) {
            return;
        }
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("sessionId", sessionId);
        data.put("requestId", requestId);
        data.put("step", event.step());
        if (event.delta() != null && !event.delta().isEmpty()) {
            data.put("delta", event.delta());
        }
        send(userId, "chat.progress", data);
    }

    private void send(Long userId, String type, Object data) {
        Map<String, Object> event = new LinkedHashMap<>();
        event.put("type", type);
        event.put("data", data);
        try {
            String payload = objectMapper.writeValueAsString(event);
            if ("chat.progress".equals(type)) {
                log.debug("[WS 推送] userId={}, type={}", userId, type);
            } else {
                log.info("[WS 推送] userId={}, type={}", userId, type);
            }
            for (WebSocketSession session : sessionRegistry.get(userId)) {
                if (!session.isOpen()) {
                    continue;
                }
                try {
                    synchronized (session) {
                        session.sendMessage(new TextMessage(payload));
                    }
                } catch (Exception ex) {
                    log.warn("WebSocket 推送失败: userId={}, sessionId={}, error={}",
                            userId, session.getId(), ex.getMessage());
                }
            }
        } catch (Exception ex) {
            log.warn("WebSocket 事件序列化失败: {}", ex.getMessage());
        }
    }

    private void sleepQuietly() {
        try {
            Thread.sleep(CHUNK_INTERVAL_MILLIS);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }
}
