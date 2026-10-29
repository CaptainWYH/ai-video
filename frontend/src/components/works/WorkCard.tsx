import { ArrowRightOutlined, ReloadOutlined } from '@ant-design/icons';
import dayjs from 'dayjs';
import type { VideoTask, VideoTaskStatus } from '@/types/video';

interface WorkCardProps {
  task: VideoTask;
  onRetry: (task: VideoTask) => void;
}

const POSTER_CLASSES = ['poster-a', 'poster-b', 'poster-c', 'poster-d'];

const STATUS_META: Record<
  VideoTaskStatus,
  { label: string; className: string; eyebrow: string }
> = {
  PENDING: { label: '排队中', className: 'is-pending', eyebrow: 'QUEUED' },
  RUNNING: { label: '生成中', className: 'is-running', eyebrow: 'GENERATING' },
  SUCCESS: { label: '已完成', className: 'is-success', eyebrow: 'COMPLETE' },
  FAILED: { label: '失败', className: 'is-failed', eyebrow: 'FAILED' },
};

const STYLE_LABELS: Record<string, string> = {
  cinematic: '电影感',
  realistic: '写实',
  anime: '动漫',
  cyberpunk: '赛博朋克',
  ink: '水墨',
};

export default function WorkCard({ task, onRetry }: WorkCardProps) {
  const meta = STATUS_META[task.status];
  const active = task.status === 'PENDING' || task.status === 'RUNNING';
  const params = task.params ?? {};
  const style = String(params.style ?? 'cinematic');
  const duration = String(params.duration ?? '5');
  const aspect = String(params.aspect ?? '16:9');
  const posterClass = POSTER_CLASSES[task.id % POSTER_CLASSES.length] ?? 'poster-a';

  return (
    <article className={`work-card ${meta.className}`}>
      <div className={`work-poster ${posterClass}`}>
        <div className="work-poster-grain" aria-hidden="true" />
        <span className={`work-status ${meta.className}`}>{meta.label}</span>
        <span className="work-poster-code">NO. {String(task.id).padStart(3, '0')}</span>
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
          <span>{duration}s</span>
          <span>{dayjs(task.createdAt).format('MM-DD HH:mm')}</span>
        </div>

        {task.status === 'FAILED' ? (
          <p className="work-error">{task.errorMessage || '生成没有完成。'}</p>
        ) : null}

        <div className="work-actions">
          {task.status === 'SUCCESS' && task.resultUrl ? (
            <a className="work-link" href={task.resultUrl} target="_blank" rel="noreferrer">
              查看结果
              <ArrowRightOutlined />
            </a>
          ) : active ? (
            <span className="work-progress-label">
              {task.status === 'PENDING' ? '等待调度' : `渲染中 ${task.progress}%`}
            </span>
          ) : (
            <button type="button" className="work-link" onClick={() => onRetry(task)}>
              <ReloadOutlined />
              重新生成
            </button>
          )}
        </div>
      </div>
    </article>
  );
}
