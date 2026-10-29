export type ChatRole = 'USER' | 'ASSISTANT';

export interface ChatSession {
  id: number;
  title: string;
  lastMessageAt: string | null;
  createdAt: string;
}

export interface ChatMessage {
  id: number;
  sessionId: number;
  role: ChatRole;
  content: string;
  messageType: string;
  videoIntent: boolean;
  toolName?: string | null;
  createdAt: string;
}

export interface SendMessagePayload {
  sessionId: number;
  content: string;
  requestId: string;
}

export interface WsEvent<T = unknown> {
  type: string;
  data: T;
}

export interface ChatChunkData {
  sessionId: number;
  requestId: string;
  delta: string;
}

export interface ChatDoneData {
  sessionId: number;
  requestId: string;
  message: ChatMessage;
}

export interface VideoProgressData {
  taskId: number;
  status: string;
  progress: number;
  resultUrl?: string | null;
}

export interface StreamingReply {
  requestId: string;
  sessionId: number;
  content: string;
}
