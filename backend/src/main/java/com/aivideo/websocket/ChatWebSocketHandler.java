package com.aivideo.websocket;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

/**
 * 对话 WebSocket 处理器。客户端可发送 "ping" 保活，服务端回复 {"type":"pong"}。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ChatWebSocketHandler extends TextWebSocketHandler {

    private final WebSocketSessionRegistry sessionRegistry;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        Long userId = (Long) session.getAttributes().get("userId");
        if (userId == null) {
            session.close(CloseStatus.NOT_ACCEPTABLE.withReason("unauthorized"));
            return;
        }
        sessionRegistry.add(userId, session);
        log.debug("WebSocket 已连接: userId={}, sessionId={}", userId, session.getId());
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        log.info("[WS 接收] userId={}, sessionId={}, payload={}",
                session.getAttributes().get("userId"), session.getId(), message.getPayload());
        if ("ping".equalsIgnoreCase(message.getPayload())) {
            String pong = "{\"type\":\"pong\"}";
            session.sendMessage(new TextMessage(pong));
            log.info("[WS 推送] userId={}, sessionId={}, payload={}",
                    session.getAttributes().get("userId"), session.getId(), pong);
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        Long userId = (Long) session.getAttributes().get("userId");
        if (userId != null) {
            sessionRegistry.remove(userId, session);
        }
        log.debug("WebSocket 已断开: userId={}, status={}", userId, status);
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        log.warn("WebSocket 传输异常: sessionId={}, error={}", session.getId(), exception.getMessage());
    }
}
