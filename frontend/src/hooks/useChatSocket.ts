import { useEffect } from 'react';
import { useChatStore } from '@/stores/chatStore';
import { tokenStorage } from '@/utils/tokenStorage';
import { getWsUrl } from '@/utils/ws';
import type { ChatChunkData, ChatDoneData, WsEvent } from '@/types/chat';

const MAX_RETRY = 6;

/**
 * 建立 /ws/chat 连接，处理 chat.chunk / chat.done 事件，断线自动重连。
 * enabled=false 时用于游客态：不连接、不触发重连。
 */
export function useChatSocket(enabled = true): void {
  const setSocketConnected = useChatStore((state) => state.setSocketConnected);

  useEffect(() => {
    if (!enabled) {
      setSocketConnected(false);
      return undefined;
    }

    let socket: WebSocket | null = null;
    let closed = false;
    let retryCount = 0;
    let retryTimer: number | undefined;
    let pingTimer: number | undefined;

    const connect = () => {
      const token = tokenStorage.getAccessToken();
      if (closed || !token) {
        return;
      }

      socket = new WebSocket(`${getWsUrl()}?token=${encodeURIComponent(token)}`);

      socket.onopen = () => {
        retryCount = 0;
        setSocketConnected(true);
        pingTimer = window.setInterval(() => {
          if (socket?.readyState === WebSocket.OPEN) {
            socket.send('ping');
          }
        }, 25_000);
      };

      socket.onmessage = (event) => {
        try {
          const payload = JSON.parse(String(event.data)) as WsEvent;
          if (payload.type === 'chat.chunk') {
            const data = payload.data as ChatChunkData;
            useChatStore.getState().appendChunk(data.sessionId, data.requestId, data.delta);
          } else if (payload.type === 'chat.done') {
            const data = payload.data as ChatDoneData;
            useChatStore
              .getState()
              .finishStreaming(data.sessionId, data.requestId, data.message);
          }
        } catch {
          // 忽略非 JSON 消息（例如 pong）
        }
      };

      socket.onclose = () => {
        setSocketConnected(false);
        if (pingTimer) {
          window.clearInterval(pingTimer);
          pingTimer = undefined;
        }
        if (closed || retryCount >= MAX_RETRY) {
          return;
        }
        retryCount += 1;
        const delay = Math.min(1000 * 2 ** retryCount, 10_000);
        retryTimer = window.setTimeout(connect, delay);
      };

      socket.onerror = () => {
        socket?.close();
      };
    };

    connect();

    return () => {
      closed = true;
      if (retryTimer) {
        window.clearTimeout(retryTimer);
      }
      if (pingTimer) {
        window.clearInterval(pingTimer);
      }
      socket?.close();
      setSocketConnected(false);
    };
  }, [enabled, setSocketConnected]);
}
