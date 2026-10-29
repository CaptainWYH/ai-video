import {
  ArrowRightOutlined,
  CheckCircleOutlined,
  ExclamationCircleOutlined,
  ReloadOutlined,
} from '@ant-design/icons';
import type { DraftParams } from '@/stores/draftStore';
import type { VideoTask } from '@/types/video';

interface GenerationCardProps {
  prompt: string;
  params: DraftParams;
  task: VideoTask | null;
  error?: string | null;
  onRetry: () => void;
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
  onWorks,
}: GenerationCardProps) {
  const status = task?.status ?? (error ? 'FAILED' : 'PENDING');
  const progress = task?.progress ?? 0;
  const active = status === 'PENDING' || status === 'RUNNING';
  const success = status === 'SUCCESS';
  const failed = status === 'FAILED';
  const statusLabel = success
    ? '已完成'
    : failed
      ? '未完成'
      : status === 'PENDING'
        ? '排队中'
        : `生成中 ${progress}%`;

  return (
    <article
      className={`gen-card${success ? ' is-success' : ''}${failed ? ' is-failed' : ''}`}
      aria-live="polite"
    >
      <div className="gen-poster">
        <div className="gen-poster-grain" aria-hidden="true" />
        <span className={`gen-status${active ? ' is-active' : ''}`}>{statusLabel}</span>
        <div className="gen-poster-center">
          <span className="gen-poster-mark" aria-hidden="true">
            AI
          </span>
          <span className="gen-poster-code">
            {task ? `GEN / ${String(task.id).padStart(3, '0')}` : 'PREPARING'}
          </span>
        </div>
        {active ? (
          <div className="gen-progress" aria-hidden="true">
            <span style={{ width: `${Math.max(progress, 4)}%` }} />
          </div>
        ) : null}
      </div>

      <div className="gen-body">
        <div className="gen-eyebrow">
          {active ? 'GENERATING' : success ? 'GENERATION COMPLETE' : 'GENERATION FAILED'}
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
            <span>任务已完成，结果链接为占位示例。</span>
          </div>
        ) : (
          <div className="gen-message">
            <span className="gen-pulse" aria-hidden="true" />
            <span>正在生成画面，离开页面不会中断任务。</span>
          </div>
        )}

        <div className="gen-actions">
          {success && task?.resultUrl ? (
            <a
              className="gen-action is-primary"
              href={task.resultUrl}
              target="_blank"
              rel="noreferrer"
            >
              查看示例结果
              <ArrowRightOutlined />
            </a>
          ) : null}
          {failed ? (
            <button type="button" className="gen-action is-primary" onClick={onRetry}>
              <ReloadOutlined />
              重新生成
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
