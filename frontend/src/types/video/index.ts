export type VideoTaskStatus = 'PENDING' | 'RUNNING' | 'SUCCESS' | 'FAILED';

export interface VideoTask {
  id: number;
  projectId?: number | null;
  prompt: string;
  params: Record<string, unknown>;
  status: VideoTaskStatus;
  progress: number;
  resultUrl?: string | null;
  errorMessage?: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface CreateVideoTaskPayload {
  prompt: string;
  params?: Record<string, unknown>;
  projectId?: number;
}
