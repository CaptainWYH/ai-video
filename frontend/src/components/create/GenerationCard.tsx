import {
  ArrowRightOutlined,
  CheckCircleOutlined,
  ExclamationCircleOutlined,
  ReloadOutlined,
} from '@ant-design/icons';
import { useEffect, useState } from 'react';
import type { DraftParams } from '@/stores/draftStore';
import type { VideoTask } from '@/types/video';

interface GenerationCardProps {
  prompt: string;
  params: DraftParams;
  task: VideoTask | null;
  error?: string | null;
  onRetry: () => void;
  onCancel?: () => void;
  onWorks: () => void;
}

const STYLE_LABELS: Record<string, string> = {
  cinematic: '电影感',
  realistic: '写实',
  anime: '动漫',
  cyberpunk: '赛博朋克',
  ink: '水墨',
};

export default function GenerationCard({
  prompt,
  params,
  task,
  error,
  onRetry,
  onCancel,
  onWorks,
}: GenerationCardProps) {
  const status = task?.status ?? (error ? 'FAILED' : 'PENDING');
  const progress = task?.progress ?? 0;
  const active = ['PENDING', 'SUBMITTING', 'QUEUED', 'RUNNING', 'DOWNLOADING'].includes(status);
  const success = status === 'SUCCESS';
  const failed = ['FAILED', 'CANCELLED', 'EXPIRED'].includes(status);
  const isMock = task?.provider?.toUpperCase() === 'MOCK';
  const providerLabel = isMock ? 'MOCK' : (task?.provider?.toUpperCase() ?? 'VIDEO');
  const playable = success && Boolean(task?.resultUrl) && !isMock;
  const [elapsedSeconds, setElapsedSeconds] = useState(0);

  useEffect(() => {
    if (!task?.createdAt) {
      setElapsedSeconds(0);
      return undefined;
    }
    const startedAt = new Date(task.createdAt).getTime();
    const update = () => {
      setElapsedSeconds(Math.max(0, Math.floor((Date.now() - startedAt) / 1000)));
    };
    update();
    if (!active) {
      return undefined;
    }
    const timer = window.setInterval(update, 1000);
    return () => window.clearInterval(timer);
  }, [active, task?.id, task?.createdAt]);

  const statusLabel =
    {
      PENDING: '准备提交',
      SUBMITTING: '提交中',
      QUEUED: '排队中',
      RUNNING: '生成中',
      DOWNLOADING: '正在保存',
      SUCCESS: '已完成',
      FAILED: '生成失败',
      CANCELLED: '已取消',
      EXPIRED: '已超时',
    }[status] ?? status;
  const usage = task?.usage;

  return (
    <article
      className={`gen-card${success ? ' is-success' : ''}${failed ? ' is-failed' : ''}`}
      aria-live="polite"
    >
      <div className="gen-poster">
        <div className="gen-poster-grain" aria-hidden="true" />
        <span className={`gen-status${active ? ' is-active' : ''}`}>
          {active ? `视频正在生成中 · ${elapsedSeconds} 秒` : statusLabel}
        </span>
        {playable && task?.resultUrl ? (
          <video
            className="gen-video"
            src={task.resultUrl}
            controls
            playsInline
            preload="metadata"
          />
        ) : (
          <div className="gen-poster-center">
            <span className="gen-poster-mark" aria-hidden="true">
              AI
            </span>
            <span className="gen-poster-code">
              {task ? `GEN / ${String(task.id).padStart(3, '0')}` : 'PREPARING'}
            </span>
          </div>
        )}
        {active ? (
          <div className="gen-progress" aria-hidden="true">
            <span style={{ width: `${Math.max(progress, 4)}%` }} />
          </div>
        ) : null}
      </div>

      <div className="gen-body">
        <div className="gen-eyebrow">
          {active
            ? `${providerLabel} / PROCESSING`
            : success
              ? `${providerLabel} / COMPLETE`
              : `${providerLabel} / FAILED`}
        </div>
        <h3 className="gen-title">{prompt}</h3>

        <div className="gen-meta">
          <span>{STYLE_LABELS[params.style] ?? params.style}</span>
          <span>{params.aspect}</span>
          <span>{params.duration}s</span>
          <span>{params.resolution}</span>
        </div>

        {failed ? (
          <div className="gen-message is-error">
            <ExclamationCircleOutlined />
            <span>{error || task?.errorMessage || '生成没有完成，可以重新提交。'}</span>
          </div>
        ) : success ? (
          <div className="gen-message is-success">
            <CheckCircleOutlined />
            <span>
              {isMock
                ? `Mock 任务已完成，用时 ${elapsedSeconds} 秒；示例地址不可播放。`
                : `视频已生成并保存，用时 ${elapsedSeconds} 秒。`}
            </span>
          </div>
        ) : (
          <div className="gen-message">
            <span className="gen-pulse" aria-hidden="true" />
            <span>
              {status === 'DOWNLOADING'
                ? `画面已生成，正在转存到作品库 · ${elapsedSeconds} 秒`
                : `视频正在生成中 · 已等待 ${elapsedSeconds} 秒`}
            </span>
          </div>
        )}

        {usage?.completionTokens ? (
          <div className="gen-usage">
            消耗 {usage.completionTokens.toLocaleString()} token · 预估 ¥
            {Number(usage.estimatedCostCny ?? 0).toFixed(4)}
          </div>
        ) : null}

        <div className="gen-actions">
          {playable && task?.resultUrl ? (
            <a
              className="gen-action is-primary"
              href={task.resultUrl}
              target="_blank"
              rel="noreferrer"
            >
              播放视频
              <ArrowRightOutlined />
            </a>
          ) : null}
          {failed ? (
            <button type="button" className="gen-action is-primary" onClick={onRetry}>
              <ReloadOutlined />
              重新生成
            </button>
          ) : null}
          {onCancel && ['PENDING', 'QUEUED'].includes(status) ? (
            <button type="button" className="gen-action" onClick={onCancel}>
              取消任务
            </button>
          ) : null}
          <button type="button" className="gen-action" onClick={onWorks}>
            去作品页
            <ArrowRightOutlined />
          </button>
        </div>
      </div>
    </article>
  );
}
