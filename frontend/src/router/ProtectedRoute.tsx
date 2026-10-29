import { Navigate, Outlet } from 'react-router-dom';
import { tokenStorage } from '@/utils/tokenStorage';

/**
 * 路由守卫：作品页需要登录。
 * 未登录时回到公开创作台，并在同页打开登录弹窗，不丢失用户当前草稿。
 */
export default function ProtectedRoute() {
  if (!tokenStorage.getAccessToken()) {
    return <Navigate to="/create?auth=works" replace />;
  }
  return <Outlet />;
}
