import { createBrowserRouter, Navigate } from 'react-router-dom';
import RootLayout from '@/layouts/RootLayout';
import WorksLayout from '@/layouts/WorksLayout';
import CreatePage from '@/pages/Create';
import LandingPage from '@/pages/Landing';
import LoginPage from '@/pages/Login';
import NotFoundPage from '@/pages/NotFound';
import RegisterPage from '@/pages/Register';
import WorksPage from '@/pages/Works';
import ProtectedRoute from '@/router/ProtectedRoute';

export const router = createBrowserRouter([
  {
    element: <RootLayout />,
    children: [
      {
        path: '/',
        element: <LandingPage />,
      },
      {
        path: '/create',
        element: <CreatePage />,
      },
      {
        path: '/login',
        element: <LoginPage />,
      },
      {
        path: '/register',
        element: <RegisterPage />,
      },
      {
        element: <ProtectedRoute />,
        children: [
          {
            element: <WorksLayout />,
            children: [{ path: '/works', element: <WorksPage /> }],
          },
        ],
      },
      // 旧地址保留兼容：/chat 进入创作台，/project 进入作品页
      { path: '/chat', element: <Navigate to="/create" replace /> },
      { path: '/project', element: <Navigate to="/works" replace /> },
      {
        path: '*',
        element: <NotFoundPage />,
      },
    ],
  },
]);
