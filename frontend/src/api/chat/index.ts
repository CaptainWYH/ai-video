import { http } from '@/api/request';
import type {
  ChatMessage,
  ChatSession,
  ConfirmToolCallPayload,
  SendMessagePayload,
  ToolConfirmResponse,
} from '@/types/chat';
import type { PageResult } from '@/types/common';

export const chatApi = {
  listSessions(page = 1, size = 50) {
    return http<PageResult<ChatSession>>({
      url: '/chat/sessions',
      method: 'GET',
      params: { page, size },
    });
  },

  createSession(title?: string) {
    return http<ChatSession>({
      url: '/chat/sessions',
      method: 'POST',
      data: title ? { title } : {},
    });
  },

  listMessages(sessionId: number, page = 1, size = 100) {
    return http<PageResult<ChatMessage>>({
      url: `/chat/sessions/${sessionId}/messages`,
      method: 'GET',
      params: { page, size },
    });
  },

  sendMessage(payload: SendMessagePayload) {
    return http<ChatMessage>({
      url: '/chat/messages',
      method: 'POST',
      data: payload,
    });
  },

  confirmToolCall(messageId: number, payload: ConfirmToolCallPayload) {
    return http<ToolConfirmResponse>({
      url: `/chat/messages/${messageId}/confirm`,
      method: 'POST',
      data: payload,
    });
  },
};
