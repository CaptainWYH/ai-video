import { create } from 'zustand';
import { chatApi } from '@/api/chat';
import type { ChatMessage, ChatSession, StreamingReply } from '@/types/chat';

const fallbackTimers = new Map<string, number>();

interface ChatState {
  sessions: ChatSession[];
  activeSessionId: number | null;
  messages: ChatMessage[];
  loadingSessions: boolean;
  loadingMessages: boolean;
  streaming: StreamingReply | null;
  socketConnected: boolean;

  setSocketConnected: (connected: boolean) => void;
  loadSessions: (autoSelect?: boolean) => Promise<void>;
  createSession: () => Promise<ChatSession>;
  selectSession: (sessionId: number) => Promise<void>;
  sendMessage: (content: string) => Promise<ChatMessage | null>;
  appendChunk: (sessionId: number, requestId: string, delta: string) => void;
  finishStreaming: (sessionId: number, requestId: string, reply: ChatMessage) => void;
  reset: () => void;
}

export const useChatStore = create<ChatState>()((set, get) => ({
  sessions: [],
  activeSessionId: null,
  messages: [],
  loadingSessions: false,
  loadingMessages: false,
  streaming: null,
  socketConnected: false,

  setSocketConnected: (connected) => set({ socketConnected: connected }),

  loadSessions: async (autoSelect = true) => {
    set({ loadingSessions: true });
    try {
      const result = await chatApi.listSessions();
      set({ sessions: result.records, loadingSessions: false });
      const { activeSessionId, sessions } = get();
      if (autoSelect && !activeSessionId && sessions.length > 0) {
        await get().selectSession(sessions[0].id);
      }
    } catch (error) {
      set({ loadingSessions: false });
      throw error;
    }
  },

  createSession: async () => {
    const session = await chatApi.createSession();
    set((state) => ({
      sessions: [session, ...state.sessions],
      activeSessionId: session.id,
      messages: [],
      streaming: null,
    }));
    return session;
  },

  selectSession: async (sessionId) => {
    set({
      activeSessionId: sessionId,
      messages: [],
      loadingMessages: true,
      streaming: null,
    });
    try {
      const result = await chatApi.listMessages(sessionId);
      // 避免快速切换会话时把旧会话消息写进当前会话
      if (get().activeSessionId === sessionId) {
        set({ messages: result.records, loadingMessages: false });
      }
    } catch (error) {
      set({ loadingMessages: false });
      throw error;
    }
  },

  sendMessage: async (content) => {
    const trimmed = content.trim();
    if (!trimmed) {
      return null;
    }

    let targetSessionId = get().activeSessionId;
    if (!targetSessionId) {
      const session = await get().createSession();
      targetSessionId = session.id;
    }

    const requestId = crypto.randomUUID();
    const optimisticMessage: ChatMessage = {
      id: -Date.now(),
      sessionId: targetSessionId,
      role: 'USER',
      content: trimmed,
      messageType: 'TEXT',
      videoIntent: false,
      createdAt: new Date().toISOString(),
    };

    set((state) => ({
      messages: [...state.messages, optimisticMessage],
      streaming: { requestId, sessionId: targetSessionId, content: '' },
    }));

    try {
      const reply = await chatApi.sendMessage({
        sessionId: targetSessionId,
        content: trimmed,
        requestId,
      });

      if (get().socketConnected) {
        // WebSocket 分片可能稍晚到达；5s 内未收到 chat.done 时用同步结果兜底
        const timer = window.setTimeout(() => {
          useChatStore.getState().finishStreaming(targetSessionId, requestId, reply);
        }, 5000);
        fallbackTimers.set(requestId, timer);
      } else {
        get().finishStreaming(targetSessionId, requestId, reply);
      }

      void get().loadSessions(false);
      return reply;
    } catch (error) {
      set((state) => ({
        messages: state.messages.filter((message) => message.id !== optimisticMessage.id),
        streaming: null,
      }));
      throw error;
    }
  },

  appendChunk: (sessionId, requestId, delta) => {
    set((state) => {
      if (
        !state.streaming ||
        state.streaming.requestId !== requestId ||
        state.streaming.sessionId !== sessionId
      ) {
        return state;
      }
      return {
        streaming: {
          ...state.streaming,
          content: state.streaming.content + delta,
        },
      };
    });
  },

  finishStreaming: (sessionId, requestId, reply) => {
    const timer = fallbackTimers.get(requestId);
    if (timer) {
      window.clearTimeout(timer);
      fallbackTimers.delete(requestId);
    }

    set((state) => {
      if (!state.streaming || state.streaming.requestId !== requestId) {
        return state;
      }
      if (state.streaming.sessionId !== sessionId) {
        return { ...state, streaming: null };
      }
      const exists = state.messages.some((message) => message.id === reply.id);
      return {
        streaming: null,
        messages: exists ? state.messages : [...state.messages, reply],
      };
    });
  },

  reset: () => {
    fallbackTimers.forEach((timer) => window.clearTimeout(timer));
    fallbackTimers.clear();
    set({
      sessions: [],
      activeSessionId: null,
      messages: [],
      loadingSessions: false,
      loadingMessages: false,
      streaming: null,
      socketConnected: false,
    });
  },
}));
