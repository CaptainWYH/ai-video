export type VideoTaskStatus =
  | 'PENDING'
  | 'SUBMITTING'
  | 'QUEUED'
  | 'RUNNING'
  | 'DOWNLOADING'
  | 'SUCCESS'
  | 'FAILED'
  | 'CANCELLED'
  | 'EXPIRED';

export interface VideoUsage {
  completionTokens?: number | null;
  totalTokens?: number | null;
  unitPriceCnyPerMillion?: number | null;
  pricingVersion?: string | null;
  estimatedCostCny?: number | null;
  currency?: string | null;
}

export interface VideoGenerationParams {
  style: string;
  aspect: string;
  duration: number;
  resolution: string;
  generateAudio: boolean;
  watermark?: boolean;
  outputFormat?: 'mp4' | 'mov';
}

export interface VideoTask {
  id: number;
  projectId?: number | null;
  prompt: string;
  params: VideoGenerationParams;
  status: VideoTaskStatus;
  progress: number;
  provider?: string | null;
  providerModel?: string | null;
  providerStatus?: string | null;
  resultUrl?: string | null;
  errorMessage?: string | null;
  usage?: VideoUsage | null;
  createdAt: string;
  updatedAt: string;
  completedAt?: string | null;
}

export interface CreateVideoTaskPayload {
  prompt: string;
  params?: VideoGenerationParams;
  projectId?: number;
  clientRequestId?: string;
}

export interface VideoUsageSummary {
  taskCount: number;
  successCount: number;
  completionTokens: number;
  estimatedCostCny: number;
  currency: string;
}
