package com.aivideo.agent;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 工具确认挂起登记表。
 *
 * <p>只保存服务端恢复 Agent 所需的信息；数据库中的 metadata 仅用于前端展示。
 * 进程内实现，重启或多实例切换后确认会失效。</p>
 */
@Component
@RequiredArgsConstructor
public class PendingConfirmationRegistry {

    private final AgentScopeProperties properties;
    private final Map<String, PendingConfirmation> pending = new ConcurrentHashMap<>();

    public void register(PendingConfirmation confirmation) {
        purgeExpired();
        pending.put(confirmation.confirmationId(), confirmation);
    }

    /**
     * 原子领取一次确认，避免重复点击导致 Agent 被恢复多次。
     *
     * @return 领取成功返回挂起记录，不存在、已过期或归属不匹配返回 null
     */
    public PendingConfirmation claim(String confirmationId, Long userId, Long messageId) {
        PendingConfirmation confirmation = pending.get(confirmationId);
        if (confirmation == null
                || confirmation.expired()
                || !confirmation.userId().equals(userId)
                || !confirmation.messageId().equals(messageId)) {
            return null;
        }
        return pending.remove(confirmationId, confirmation) ? confirmation : null;
    }

    public boolean hasPending(Long userId, Long sessionId) {
        purgeExpired();
        return pending.values().stream()
                .anyMatch(item -> item.userId().equals(userId) && item.sessionId().equals(sessionId));
    }

    public Instant expiresAt() {
        return Instant.now().plus(properties.getConfirmTimeout());
    }

    private void purgeExpired() {
        Instant now = Instant.now();
        pending.entrySet().removeIf(entry -> entry.getValue().expiresAt().isBefore(now));
    }
}
