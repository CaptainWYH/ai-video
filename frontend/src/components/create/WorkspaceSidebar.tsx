import {
  AppstoreOutlined,
  LoginOutlined,
  LogoutOutlined,
  MessageOutlined,
  PlusOutlined,
} from '@ant-design/icons';
import { Empty, Spin } from 'antd';
import dayjs from 'dayjs';
import Brand from '@/components/common/Brand';
import type { ChatSession } from '@/types/chat';
import type { UserInfo } from '@/types/auth';

interface WorkspaceSidebarProps {
  guest: boolean;
  user: UserInfo | null;
  sessions: ChatSession[];
  activeSessionId: number | null;
  loading: boolean;
  creating: boolean;
  onNew: () => void;
  onSelect: (sessionId: number) => void;
  onLogin: () => void;
  onLogout: () => void;
  onWorks: () => void;
}

export default function WorkspaceSidebar({
  guest,
  user,
  sessions,
  activeSessionId,
  loading,
  creating,
  onNew,
  onSelect,
  onLogin,
  onLogout,
  onWorks,
}: WorkspaceSidebarProps) {
  const displayName = user?.nickname || user?.username || '创作者';
  const today = sessions.filter((session) =>
    dayjs(session.lastMessageAt ?? session.createdAt).isSame(dayjs(), 'day'),
  );
  const earlier = sessions.filter(
    (session) => !dayjs(session.lastMessageAt ?? session.createdAt).isSame(dayjs(), 'day'),
  );

  const renderSession = (session: ChatSession) => (
    <button
      key={session.id}
      type="button"
      className={`ws-session${session.id === activeSessionId ? ' is-active' : ''}`}
      onClick={() => onSelect(session.id)}
    >
      <span className="ws-session-icon">
        <MessageOutlined />
      </span>
      <span className="ws-session-copy">
        <span className="ws-session-title">{session.title}</span>
        <span className="ws-session-time">
          {dayjs(session.lastMessageAt ?? session.createdAt).format('MM-DD HH:mm')}
        </span>
      </span>
    </button>
  );

  return (
    <div className="ws-sidebar">
      <div className="ws-sidebar-brand">
        <Brand to="/" compact />
      </div>

      <button
        type="button"
        className="ws-new-btn"
        onClick={onNew}
        disabled={creating}
      >
        <PlusOutlined />
        <span>新的创作</span>
      </button>

      <div className="ws-session-section">
        <div className="ws-section-label">
          <span>最近创作</span>
          {loading ? <Spin size="small" /> : null}
        </div>

        <div className="ws-session-list">
          {guest ? (
            <div className="ws-guest-hint">
              <MessageOutlined />
              <p>登录后，对话与分镜会保存在这里。</p>
            </div>
          ) : sessions.length === 0 && !loading ? (
            <Empty
              image={Empty.PRESENTED_IMAGE_SIMPLE}
              description="还没有会话"
              className="ws-session-empty"
            />
          ) : (
            <>
              {today.length > 0 ? (
                <>
                  <div className="ws-group-label">今天</div>
                  {today.map(renderSession)}
                </>
              ) : null}
              {earlier.length > 0 ? (
                <>
                  <div className="ws-group-label">更早</div>
                  {earlier.map(renderSession)}
                </>
              ) : null}
            </>
          )}
        </div>
      </div>

      <div className="ws-sidebar-footer">
        {guest ? (
          <div className="ws-guest-card">
            <div className="ws-guest-eyebrow">GUEST MODE</div>
            <div className="ws-guest-title">登录后保存创作</div>
            <p>先写下想法。登录后，对话与分镜都会保存在账号里。</p>
            <button type="button" className="ws-guest-login" onClick={onLogin}>
              <LoginOutlined />
              登录 / 注册
            </button>
          </div>
        ) : (
          <>
            <button type="button" className="ws-footer-link" onClick={onWorks}>
              <AppstoreOutlined />
              我的作品
            </button>
            <div className="ws-user-row">
              <span className="ws-user-avatar" aria-hidden="true">
                {displayName.slice(0, 1).toUpperCase()}
              </span>
              <span className="ws-user-copy">
                <span className="ws-user-name">{displayName}</span>
                <span className="ws-user-email">{user?.email}</span>
              </span>
              <button
                type="button"
                className="ws-logout-btn"
                onClick={onLogout}
                aria-label="退出登录"
              >
                <LogoutOutlined />
              </button>
            </div>
          </>
        )}
      </div>
    </div>
  );
}
