import { AppstoreOutlined, LogoutOutlined, MenuOutlined, UserOutlined } from '@ant-design/icons';
import { Avatar, Dropdown, type MenuProps } from 'antd';
import SocketBadge from '@/components/common/SocketBadge';
import ThemeToggle from '@/components/common/ThemeToggle';
import type { UserInfo } from '@/types/auth';

interface WorkspaceTopBarProps {
  title: string;
  subtitle: string;
  socketConnected: boolean;
  guest: boolean;
  user: UserInfo | null;
  onMenu: () => void;
  onLogin: () => void;
  onLogout: () => void;
  onWorks: () => void;
}

export default function WorkspaceTopBar({
  title,
  subtitle,
  socketConnected,
  guest,
  user,
  onMenu,
  onLogin,
  onLogout,
  onWorks,
}: WorkspaceTopBarProps) {
  const displayName = user?.nickname || user?.username || '创作者';
  const menuItems: MenuProps['items'] = [
    {
      key: 'profile',
      disabled: true,
      label: (
        <div className="user-menu-profile">
          <div className="user-menu-name">{displayName}</div>
          <div className="user-menu-email">{user?.email}</div>
        </div>
      ),
    },
    { type: 'divider' },
    {
      key: 'logout',
      icon: <LogoutOutlined />,
      danger: true,
      label: '退出登录',
    },
  ];

  const handleMenuClick: MenuProps['onClick'] = ({ key }) => {
    if (key === 'logout') {
      onLogout();
    }
  };

  return (
    <header className="create-topbar">
      <div className="create-topbar-left">
        <button
          type="button"
          className="ws-menu-btn"
          onClick={onMenu}
          aria-label="打开会话列表"
        >
          <MenuOutlined />
        </button>
        <div className="ws-heading">
          <div className="ws-heading-title">{title}</div>
          <div className="ws-heading-sub">{subtitle}</div>
        </div>
        {!guest ? <SocketBadge connected={socketConnected} /> : null}
      </div>

      <div className="create-topbar-right">
        <ThemeToggle />
        <button type="button" className="ws-top-action" onClick={onWorks}>
          <AppstoreOutlined />
          <span>我的作品</span>
        </button>

        {guest ? (
          <button type="button" className="ws-login-btn" onClick={onLogin}>
            登录
          </button>
        ) : (
          <Dropdown menu={{ items: menuItems, onClick: handleMenuClick }} trigger={['click']}>
            <button type="button" className="ws-user-trigger">
              <Avatar size={30} icon={<UserOutlined />} />
              <span className="ws-user-name">{displayName}</span>
            </button>
          </Dropdown>
        )}
      </div>
    </header>
  );
}
