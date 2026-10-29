import { Outlet } from 'react-router-dom';
import AuthModal from '@/components/auth/AuthModal';

/**
 * 全局路由外壳。AuthModal 需要处在 Router 上下文内，
 * 因为品牌标识和登录成功后的挂起动作都会用到路由能力。
 */
export default function RootLayout() {
  return (
    <>
      <Outlet />
      <AuthModal />
    </>
  );
}
