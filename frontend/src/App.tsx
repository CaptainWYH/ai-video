import { App as AntdApp, ConfigProvider } from 'antd';
import zhCN from 'antd/locale/zh_CN';
import { RouterProvider } from 'react-router-dom';
import { router } from '@/router';
import { useThemeStore } from '@/stores/themeStore';
import { darkThemeConfig, lightThemeConfig } from '@/theme/themeConfig';

export default function App() {
  const mode = useThemeStore((state) => state.mode);

  return (
    <ConfigProvider
      locale={zhCN}
      theme={mode === 'dark' ? darkThemeConfig : lightThemeConfig}
    >
      <AntdApp>
        <RouterProvider router={router} />
      </AntdApp>
    </ConfigProvider>
  );
}
