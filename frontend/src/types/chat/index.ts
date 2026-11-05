export type ChatRole = 'USER' | 'ASSISTANT';

export interface ChatSession {
  id: number;
  title: string;
  lastMessageAt: string | null;
  createdAt: string;
}

export type ConfirmStatus = 'PENDING' | 'APPROVED' | 'DENIED' | 'EXPIRED' | 'FAILED';

export interface ToolCallInfo {
  id: string;
  name: string;
  input: Record<string, unknown>;
}

export interface ChatMessage {
  id: number;
  sessionId: number;
  role: ChatRole;
  content: string;
  messageType: string;
  videoIntent: boolean;
  toolName?: string | null;
  confirmationId?: string | null;
  replyId?: string | null;
  toolCalls?: ToolCallInfo[] | null;
  confirmStatus?: ConfirmStatus | null;
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

export interface ToolConfirmRequiredData {
  sessionId: number;
  requestId: string;
  message: ChatMessage;
}

export interface ToolConfirmResultData {
  sessionId: number;
  requestId: string;
  messageId: number;
  confirmationId: string;
  replyId: string;
  status: ConfirmStatus;
  toolCalls: ToolCallInfo[];
}

export interface ConfirmToolCallPayload {
  approved: boolean;
  requestId: string;
}

export interface ToolConfirmResponse {
  confirmMessage: ChatMessage;
  replyMessage: ChatMessage;
}
