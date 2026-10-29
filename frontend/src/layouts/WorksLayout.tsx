import { LogoutOutlined, UserOutlined } from '@ant-design/icons';
import { Avatar, Dropdown, message, type MenuProps } from 'antd';
import { useEffect } from 'react';
import { Link, NavLink, Outlet, useNavigate } from 'react-router-dom';
import Brand from '@/components/common/Brand';
import ThemeToggle from '@/components/common/ThemeToggle';
import { useAuthStore } from '@/stores/authStore';
import { useChatStore } from '@/stores/chatStore';

export default function WorksLayout() {
  const navigate = useNavigate();
  const user = useAuthStore((state) => state.user);
  const fetchMe = useAuthStore((state) => state.fetchMe);
  const logout = useAuthStore((state) => state.logout);
  const resetChat = useChatStore((state) => state.reset);

  useEffect(() => {
    if (!user) {
      void fetchMe();
    }
  }, [user, fetchMe]);

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
    if (key !== 'logout') {
      return;
    }
    void (async () => {
      await logout();
      resetChat();
      message.success('已退出登录');
      navigate('/', { replace: true });
    })();
  };

  return (
    <div className="works-layout">
      <header className="works-header">
        <Brand to="/create" />
        <nav className="works-nav" aria-label="主导航">
          <NavLink
            to="/create"
            className={({ isActive }) => `works-nav-link${isActive ? ' is-active' : ''}`}
          >
            创作
          </NavLink>
          <NavLink
            to="/works"
            className={({ isActive }) => `works-nav-link${isActive ? ' is-active' : ''}`}
          >
            我的作品
          </NavLink>
        </nav>
        <div className="works-header-right">
          <ThemeToggle />
          <Link to="/create" className="works-new-link">
            新建创作
          </Link>
          <Dropdown menu={{ items: menuItems, onClick: handleMenuClick }} trigger={['click']}>
            <button type="button" className="ws-user-trigger">
              <Avatar size={30} icon={<UserOutlined />} />
              <span className="ws-user-name">{displayName}</span>
            </button>
          </Dropdown>
        </div>
      </header>

      <main className="works-main">
        <Outlet />
      </main>
    </div>
  );
}
