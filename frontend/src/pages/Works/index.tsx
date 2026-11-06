import { PlusOutlined, ReloadOutlined } from '@ant-design/icons';
import { Button, Spin, message } from 'antd';
import { useCallback, useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import WorkCard from '@/components/works/WorkCard';
import { videoApi } from '@/api/video';
import { getErrorMessage } from '@/utils/error';
import type { VideoTask, VideoUsageSummary } from '@/types/video';

type WorkFilter = 'ALL' | 'ACTIVE' | 'SUCCESS' | 'FAILED';

const FILTERS: { key: WorkFilter; label: string }[] = [
  { key: 'ALL', label: '全部' },
  { key: 'ACTIVE', label: '生成中' },
  { key: 'SUCCESS', label: '已完成' },
  { key: 'FAILED', label: '失败' },
];

function matchesFilter(task: VideoTask, filter: WorkFilter): boolean {
  if (filter === 'ALL') {
    return true;
  }
  if (filter === 'ACTIVE') {
    return ['PENDING', 'SUBMITTING', 'QUEUED', 'RUNNING', 'DOWNLOADING'].includes(task.status);
  }
  return task.status === filter;
}

export default function WorksPage() {
  const navigate = useNavigate();
  const [tasks, setTasks] = useState<VideoTask[]>([]);
  const [usage, setUsage] = useState<VideoUsageSummary | null>(null);
  const [loading, setLoading] = useState(true);
  const [filter, setFilter] = useState<WorkFilter>('ALL');

  const loadTasks = useCallback(async (silent = false) => {
    if (!silent) {
      setLoading(true);
    }
    try {
      const [result, summary] = await Promise.all([
        videoApi.listTasks(1, 100),
        videoApi.usageSummary(),
      ]);
      setTasks(result.records);
      setUsage(summary);
    } catch (error) {
      if (!silent) {
        message.error(getErrorMessage(error));
      }
    } finally {
      if (!silent) {
        setLoading(false);
      }
    }
  }, []);

  useEffect(() => {
    void loadTasks();
  }, [loadTasks]);

  const hasActiveTask = tasks.some(
    (task) =>
      ['PENDING', 'SUBMITTING', 'QUEUED', 'RUNNING', 'DOWNLOADING'].includes(task.status),
  );

  useEffect(() => {
    if (!hasActiveTask) {
      return undefined;
    }
    const timer = window.setInterval(() => {
      void loadTasks(true);
    }, 2500);
    return () => window.clearInterval(timer);
  }, [hasActiveTask, loadTasks]);

  const handleRetry = async (task: VideoTask) => {
    try {
      await videoApi.retryTask(task.id);
      message.success('已重新提交生成');
      await loadTasks(true);
    } catch (error) {
      message.error(getErrorMessage(error));
    }
  };

  const handleCancel = async (task: VideoTask) => {
    try {
      await videoApi.cancelTask(task.id);
      message.success('已取消视频任务');
      await loadTasks(true);
    } catch (error) {
      message.error(getErrorMessage(error));
    }
  };

  const counts = {
    ALL: tasks.length,
    ACTIVE: tasks.filter((task) => matchesFilter(task, 'ACTIVE')).length,
    SUCCESS: tasks.filter((task) => matchesFilter(task, 'SUCCESS')).length,
    FAILED: tasks.filter((task) => matchesFilter(task, 'FAILED')).length,
  };

  const visibleTasks = tasks.filter((task) => matchesFilter(task, filter));

  return (
    <div className="works-page">
      <section className="works-hero">
        <div>
          <div className="eyebrow">
            <span className="eyebrow-line" />
            ARCHIVE / 作品
          </div>
          <h1>我的作品</h1>
          <p>每一次生成都留下记录。提示词、参数和结果都在这里。</p>
          {usage ? (
            <div className="works-usage">
              <span>{usage.taskCount} 个任务</span>
              <span>{usage.completionTokens.toLocaleString()} token</span>
              <span>预估 ¥{Number(usage.estimatedCostCny).toFixed(4)}</span>
            </div>
          ) : null}
        </div>
        <div className="works-hero-actions">
          <Button
            icon={<ReloadOutlined />}
            loading={loading}
            onClick={() => void loadTasks()}
          >
            刷新
          </Button>
          <Button type="primary" icon={<PlusOutlined />} onClick={() => navigate('/create')}>
            新建创作
          </Button>
        </div>
      </section>

      <div className="works-toolbar">
        <div className="works-filters" role="tablist" aria-label="作品筛选">
          {FILTERS.map((item) => (
            <button
              key={item.key}
              type="button"
              role="tab"
              aria-selected={filter === item.key}
              className={`works-filter${filter === item.key ? ' is-active' : ''}`}
              onClick={() => setFilter(item.key)}
            >
              {item.label}
              <span>{counts[item.key]}</span>
            </button>
          ))}
        </div>
        {hasActiveTask ? (
          <span className="works-live">
            <span className="works-live-dot" aria-hidden="true" />
            自动刷新中
          </span>
        ) : null}
      </div>

      {loading ? (
        <div className="works-loading">
          <Spin size="large" />
          <span>正在载入作品…</span>
        </div>
      ) : tasks.length === 0 ? (
        <div className="works-empty">
          <div className="works-empty-mark" aria-hidden="true">
            AI
          </div>
          <div className="eyebrow">
            <span className="eyebrow-line" />
            NO TAKES YET
          </div>
          <h2>还没有作品。</h2>
          <p>回到创作台写下第一个画面，生成记录会自动出现在这里。</p>
          <Button type="primary" onClick={() => navigate('/create')}>
            开始创作
          </Button>
        </div>
      ) : visibleTasks.length === 0 ? (
        <div className="works-empty is-compact">
          <h2>这个筛选下没有作品。</h2>
          <p>换一个状态看看，或者回到创作台提交新的生成任务。</p>
          <Button onClick={() => setFilter('ALL')}>查看全部</Button>
        </div>
      ) : (
        <div className="works-grid">
          {visibleTasks.map((task) => (
            <WorkCard
              key={task.id}
              task={task}
              onRetry={handleRetry}
              onCancel={handleCancel}
            />
          ))}
        </div>
      )}
    </div>
  );
}
