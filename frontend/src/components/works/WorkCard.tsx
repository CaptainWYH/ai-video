import { ArrowRightOutlined, ReloadOutlined } from '@ant-design/icons';
import dayjs from 'dayjs';
import type { VideoTask, VideoTaskStatus } from '@/types/video';

interface WorkCardProps {
  task: VideoTask;
  onRetry: (task: VideoTask) => void;
  onCancel: (task: VideoTask) => void;
}

const POSTER_CLASSES = ['poster-a', 'poster-b', 'poster-c', 'poster-d'];

const STATUS_META: Record<
  VideoTaskStatus,
  { label: string; className: string; eyebrow: string }
> = {
  PENDING: { label: '排队中', className: 'is-pending', eyebrow: 'QUEUED' },
  SUBMITTING: { label: '提交中', className: 'is-pending', eyebrow: 'SUBMITTING' },
  QUEUED: { label: '排队中', className: 'is-pending', eyebrow: 'QUEUED' },
  RUNNING: { label: '生成中', className: 'is-running', eyebrow: 'GENERATING' },
  DOWNLOADING: { label: '保存中', className: 'is-running', eyebrow: 'SAVING' },
  SUCCESS: { label: '已完成', className: 'is-success', eyebrow: 'COMPLETE' },
  FAILED: { label: '失败', className: 'is-failed', eyebrow: 'FAILED' },
  CANCELLED: { label: '已取消', className: 'is-failed', eyebrow: 'CANCELLED' },
  EXPIRED: { label: '已超时', className: 'is-failed', eyebrow: 'EXPIRED' },
};

const STYLE_LABELS: Record<string, string> = {
  cinematic: '电影感',
  realistic: '写实',
  anime: '动漫',
  cyberpunk: '赛博朋克',
  ink: '水墨',
};

export default function WorkCard({ task, onRetry, onCancel }: WorkCardProps) {
  const meta = STATUS_META[task.status];
  const active = ['PENDING', 'SUBMITTING', 'QUEUED', 'RUNNING', 'DOWNLOADING'].includes(
    task.status,
  );
  const params = task.params;
  const isMock = task.provider?.toUpperCase() === 'MOCK';
  const playable = task.status === 'SUCCESS' && Boolean(task.resultUrl) && !isMock;
  const style = params.style;
  const duration = params.duration;
  const aspect = params.aspect;
  const posterClass = POSTER_CLASSES[task.id % POSTER_CLASSES.length] ?? 'poster-a';

  return (
    <article className={`work-card ${meta.className}`}>
      <div className={`work-poster ${posterClass}`}>
        <div className="work-poster-grain" aria-hidden="true" />
        <span className={`work-status ${meta.className}`}>{meta.label}</span>
        <span className="work-poster-code">NO. {String(task.id).padStart(3, '0')}</span>
        {playable && task.resultUrl ? (
          <video
            className="work-video"
            src={task.resultUrl}
            controls
            playsInline
            preload="metadata"
          />
        ) : null}
        {active ? (
          <div className="work-progress" aria-hidden="true">
            <span style={{ width: `${Math.max(task.progress, 4)}%` }} />
          </div>
        ) : null}
        {task.status === 'SUCCESS' ? (
          <span className="work-poster-done" aria-hidden="true">
            ✓
          </span>
        ) : null}
      </div>

      <div className="work-body">
        <div className="work-eyebrow">{meta.eyebrow}</div>
        <h3 className="work-prompt">{task.prompt}</h3>
        <div className="work-meta">
          <span>{STYLE_LABELS[style] ?? style}</span>
          <span>{aspect}</span>
          <span>
            {duration}s · {params.resolution}
          </span>
          <span>{dayjs(task.createdAt).format('MM-DD HH:mm')}</span>
        </div>

        {(['FAILED', 'CANCELLED', 'EXPIRED'] as string[]).includes(task.status) ? (
          <p className="work-error">{task.errorMessage || '生成没有完成。'}</p>
        ) : null}

        {task.usage?.completionTokens ? (
          <p className="work-usage">
            {task.usage.completionTokens.toLocaleString()} token · 预估 ¥
            {Number(task.usage.estimatedCostCny ?? 0).toFixed(4)}
          </p>
        ) : null}

        <div className="work-actions">
          {playable && task.resultUrl ? (
            <a className="work-link" href={task.resultUrl} target="_blank" rel="noreferrer">
              播放视频
              <ArrowRightOutlined />
            </a>
          ) : task.status === 'SUCCESS' && isMock ? (
            <span className="work-progress-label">Mock 示例结果</span>
          ) : active ? (
            <span className="work-progress-label">{meta.label}</span>
          ) : (
            <button type="button" className="work-link" onClick={() => onRetry(task)}>
              <ReloadOutlined />
              重新生成
            </button>
          )}
          {['PENDING', 'QUEUED'].includes(task.status) ? (
            <button type="button" className="work-link" onClick={() => onCancel(task)}>
              取消任务
            </button>
          ) : null}
        </div>
      </div>
    </article>
  );
}
