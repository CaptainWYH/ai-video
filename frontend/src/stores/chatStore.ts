import { create } from 'zustand';
import { chatApi } from '@/api/chat';
import type {
  AgentTraceStep,
  ChatMessage,
  ChatProgressData,
  ChatSession,
  LiveTrace,
  StreamingReply,
  ToolConfirmRequiredData,
  ToolConfirmResultData,
  VideoProgressData,
} from '@/types/chat';
import type { VideoGenerationParams, VideoTaskStatus } from '@/types/video';

const fallbackTimers = new Map<string, number>();

function clearFallbackTimer(requestId: string) {
  const timer = fallbackTimers.get(requestId);
  if (timer) {
    window.clearTimeout(timer);
    fallbackTimers.delete(requestId);
  }
}

interface ChatState {
  sessions: ChatSession[];
  activeSessionId: number | null;
  messages: ChatMessage[];
  loadingSessions: boolean;
  loadingMessages: boolean;
  streaming: StreamingReply | null;
  liveTrace: LiveTrace | null;
  socketConnected: boolean;
  confirmingMessageId: number | null;
  decidingPromptMessageId: number | null;

  setSocketConnected: (connected: boolean) => void;
  loadSessions: (autoSelect?: boolean) => Promise<void>;
  createSession: () => Promise<ChatSession>;
  selectSession: (sessionId: number) => Promise<void>;
  sendMessage: (content: string, params?: VideoGenerationParams) => Promise<ChatMessage | null>;
  appendChunk: (sessionId: number, requestId: string, delta: string) => void;
  handleProgress: (data: ChatProgressData) => void;
  finishStreaming: (sessionId: number, requestId: string, reply: ChatMessage) => void;
  upsertMessage: (message: ChatMessage) => void;
  handleToolConfirmRequired: (data: ToolConfirmRequiredData) => void;
  handleToolConfirmResult: (data: ToolConfirmResultData) => void;
  handleVideoProgress: (data: VideoProgressData) => void;
  confirmToolCall: (messageId: number, approved: boolean) => Promise<ChatMessage | null>;
  decidePrompt: (messageId: number, useOptimizedPrompt: boolean) => Promise<ChatMessage | null>;
  reset: () => void;
}

export const useChatStore = create<ChatState>()((set, get) => ({
  sessions: [],
  activeSessionId: null,
  messages: [],
  loadingSessions: false,
  loadingMessages: false,
  streaming: null,
  liveTrace: null,
  socketConnected: false,
  confirmingMessageId: null,
  decidingPromptMessageId: null,

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
      liveTrace: null,
      confirmingMessageId: null,
    }));
    return session;
  },

  selectSession: async (sessionId) => {
    set({
      activeSessionId: sessionId,
      messages: [],
      loadingMessages: true,
      streaming: null,
      liveTrace: null,
      confirmingMessageId: null,
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

  sendMessage: async (content, params) => {
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
      liveTrace: { requestId, sessionId: targetSessionId, steps: [] },
    }));

    try {
      const reply = await chatApi.sendMessage({
        sessionId: targetSessionId,
        content: trimmed,
        requestId,
        params,
      });

      if (reply.messageType === 'TOOL_CONFIRM') {
        // 工具确认是本轮终点：立即结束占位气泡，不设置 chat.done 兜底。
        clearFallbackTimer(requestId);
        get().handleToolConfirmRequired({
          sessionId: targetSessionId,
          requestId,
          message: reply,
        });
        void get().loadSessions(false);
        return reply;
      }

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
        liveTrace: state.liveTrace?.requestId === requestId ? null : state.liveTrace,
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

  handleProgress: (data) => {
    set((state) => {
      const live = state.liveTrace;
      if (
        !live ||
        live.sessionId !== data.sessionId ||
        live.requestId !== data.requestId ||
        state.activeSessionId !== data.sessionId ||
        !data.step?.id
      ) {
        return state;
      }

      const incoming = data.step;
      const delta = data.delta ?? '';
      const index = live.steps.findIndex((step) => step.id === incoming.id);
      if (index === -1) {
        const created: AgentTraceStep = { ...incoming };
        if (delta) {
          if (created.type === 'TOOL_RESULT') {
            created.result = (created.result ?? '') + delta;
          } else {
            created.detail = (created.detail ?? '') + delta;
          }
        }
        return { liveTrace: { ...live, steps: [...live.steps, created] } };
      }

      const current = live.steps[index];
      const merged: AgentTraceStep = { ...current, ...incoming };
      // 非增量帧带来的是聚合后的快照；增量帧只追加，不能覆盖已累积内容。
      merged.detail = incoming.detail ?? current.detail;
      merged.result = incoming.result ?? current.result;
      if (delta) {
        const type = incoming.type ?? current.type;
        if (type === 'TOOL_RESULT') {
          merged.result = (current.result ?? '') + delta;
        } else {
          merged.detail = (current.detail ?? '') + delta;
        }
      }
      const steps = [...live.steps];
      steps[index] = merged;
      return { liveTrace: { ...live, steps } };
    });
  },

  finishStreaming: (sessionId, requestId, reply) => {
    clearFallbackTimer(requestId);

    set((state) => {
      const clearTrace = state.liveTrace?.requestId === requestId ? { liveTrace: null } : {};
      if (!state.streaming || state.streaming.requestId !== requestId) {
        return Object.keys(clearTrace).length ? { ...state, ...clearTrace } : state;
      }
      if (state.streaming.sessionId !== sessionId) {
        return { ...state, ...clearTrace, streaming: null };
      }
      const exists = state.messages.some((message) => message.id === reply.id);
      return {
        ...clearTrace,
        streaming: null,
        messages: exists ? state.messages : [...state.messages, reply],
      };
    });
  },

  upsertMessage: (message) => {
    set((state) => {
      const index = state.messages.findIndex((item) => item.id === message.id);
      if (index === -1) {
        return { messages: [...state.messages, message] };
      }
      const messages = [...state.messages];
      messages[index] = { ...messages[index], ...message };
      return { messages };
    });
  },

  handleToolConfirmRequired: (data) => {
    if (get().activeSessionId !== data.sessionId) {
      return;
    }
    clearFallbackTimer(data.requestId);
    set((state) => {
      const exists = state.messages.some((message) => message.id === data.message.id);
      return {
        streaming: null,
        liveTrace: state.liveTrace?.requestId === data.requestId ? null : state.liveTrace,
        messages: exists
          ? state.messages.map((message) =>
              message.id === data.message.id ? { ...message, ...data.message } : message,
            )
          : [...state.messages, data.message],
      };
    });
  },

  handleToolConfirmResult: (data) => {
    if (get().activeSessionId !== data.sessionId) {
      return;
    }
    set((state) => ({
      messages: state.messages.map((message) =>
        message.id === data.messageId
          ? {
              ...message,
              confirmationId: data.confirmationId,
              confirmStatus: data.status,
              toolCalls: data.toolCalls,
            }
          : message,
      ),
    }));
  },

  handleVideoProgress: (data) => {
    if (data.sessionId && get().activeSessionId !== data.sessionId) {
      return;
    }
    set((state) => ({
      messages: state.messages.map((message) => {
        if (message.videoTask?.id !== data.taskId) {
          return message;
        }
        return {
          ...message,
          videoTask: {
            ...message.videoTask,
            status: data.status as VideoTaskStatus,
            progress: data.progress ?? message.videoTask.progress,
            resultUrl: data.resultUrl ?? message.videoTask.resultUrl,
            usage: data.usage ?? message.videoTask.usage,
          },
        };
      }),
    }));
  },

  confirmToolCall: async (messageId, approved) => {
    const message = get().messages.find((item) => item.id === messageId);
    if (!message) {
      return null;
    }

    const requestId = crypto.randomUUID();
    set({
      confirmingMessageId: messageId,
      liveTrace: { requestId, sessionId: message.sessionId, steps: [] },
    });
    try {
      const response = await chatApi.confirmToolCall(messageId, { approved, requestId });
      get().upsertMessage(response.confirmMessage);

      if (response.replyMessage.messageType === 'TOOL_CONFIRM') {
        // Agent 恢复后再次调用工具，出现新的确认卡片。
        get().handleToolConfirmRequired({
          sessionId: response.replyMessage.sessionId,
          requestId,
          message: response.replyMessage,
        });
        void get().loadSessions(false);
        return response.replyMessage;
      }

      get().upsertMessage(response.replyMessage);
      set({ streaming: null });
      void get().loadSessions(false);
      return response.replyMessage;
    } catch (error) {
      set((state) => ({
        messages: state.messages.map((item) =>
          item.id === messageId && item.confirmStatus === 'PENDING'
            ? { ...item, confirmStatus: 'FAILED' }
            : item,
        ),
      }));
      throw error;
    } finally {
      set((state) => ({
        confirmingMessageId: null,
        liveTrace: state.liveTrace?.requestId === requestId ? null : state.liveTrace,
      }));
    }
  },

  decidePrompt: async (messageId, useOptimizedPrompt) => {
    set({ decidingPromptMessageId: messageId });
    try {
      const response = await chatApi.decidePrompt(messageId, { useOptimizedPrompt });
      get().upsertMessage(response.proposalMessage);
      get().upsertMessage(response.videoMessage);
      void get().loadSessions(false);
      return response.videoMessage;
    } finally {
      set({ decidingPromptMessageId: null });
    }
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
      liveTrace: null,
      socketConnected: false,
      confirmingMessageId: null,
      decidingPromptMessageId: null,
    });
  },
}));
