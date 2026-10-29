package com.aivideo.websocket;

import com.aivideo.security.JwtTokenProvider;
import com.aivideo.security.TokenStore;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Map;

/**
 * WebSocket 握手鉴权：token 通过查询参数传入，例如
 * {@code ws://localhost:8080/ws/chat?token=<accessToken>}。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ChatHandshakeInterceptor implements HandshakeInterceptor {

    private final JwtTokenProvider jwtTokenProvider;
    private final TokenStore tokenStore;

    @Override
    public boolean beforeHandshake(ServerHttpRequest request,
                                   ServerHttpResponse response,
                                   WebSocketHandler wsHandler,
                                   Map<String, Object> attributes) {
        if (!(request instanceof ServletServerHttpRequest servletRequest)) {
            return false;
        }
        String token = UriComponentsBuilder.fromUri(servletRequest.getURI())
                .build()
                .getQueryParams()
                .getFirst("token");
        if (token == null || token.isBlank()) {
            log.debug("WebSocket 握手缺少 token");
            return false;
        }
        try {
            Claims claims = jwtTokenProvider.parse(token);
            if (!jwtTokenProvider.isType(claims, JwtTokenProvider.TYPE_ACCESS)
                    || tokenStore.isAccessTokenBlacklisted(claims.getId())) {
                return false;
            }
            attributes.put("userId", jwtTokenProvider.getUserId(claims));
            attributes.put("username", claims.get("username", String.class));
            return true;
        } catch (Exception ex) {
            log.debug("WebSocket 握手 token 无效: {}", ex.getMessage());
            return false;
        }
    }

    @Override
    public void afterHandshake(ServerHttpRequest request,
                               ServerHttpResponse response,
                               WebSocketHandler wsHandler,
                               Exception exception) {
        // no-op
    }
}
