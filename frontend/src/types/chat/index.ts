import type { VideoGenerationParams, VideoTask, VideoUsage } from '@/types/video';

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

export interface PromptProposal {
  originalPrompt: string;
  optimizedPrompt: string;
  analysisSteps: string[];
  toolName: string;
}

export type AgentTraceType = 'STATUS' | 'THINKING' | 'TOOL_CALL' | 'TOOL_RESULT' | 'ERROR';

export type AgentTraceStatus = 'RUNNING' | 'SUCCESS' | 'ERROR' | 'PENDING' | 'DENIED';

export interface AgentTraceStep {
  id: string;
  type: AgentTraceType;
  title?: string | null;
  status?: AgentTraceStatus | null;
  detail?: string | null;
  result?: string | null;
  toolName?: string | null;
  toolCallId?: string | null;
  startedAt?: string | null;
  endedAt?: string | null;
}

export interface ChatProgressData {
  sessionId: number;
  requestId: string;
  step: AgentTraceStep;
  delta?: string | null;
}

export interface LiveTrace {
  requestId: string;
  sessionId: number;
  steps: AgentTraceStep[];
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
  promptProposal?: PromptProposal | null;
  promptDecisionStatus?: 'PENDING' | 'SELECTED' | null;
  useOptimizedPrompt?: boolean | null;
  videoTask?: VideoTask | null;
  reasoningSteps?: AgentTraceStep[] | null;
  createdAt: string;
}

export interface SendMessagePayload {
  sessionId: number;
  content: string;
  requestId: string;
  params?: VideoGenerationParams;
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
  sessionId?: number | null;
  messageId?: number | null;
  status: string;
  progress?: number | null;
  resultUrl?: string | null;
  usage?: VideoUsage | null;
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

export interface PromptDecisionPayload {
  useOptimizedPrompt: boolean;
}

export interface PromptDecisionResponse {
  proposalMessage: ChatMessage;
  videoMessage: ChatMessage;
}
