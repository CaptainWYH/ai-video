import axios, { type AxiosError, type AxiosRequestConfig } from 'axios';
import { message } from 'antd';
import { tokenStorage } from '@/utils/tokenStorage';
import type { ApiResult } from '@/types/common';

export class ApiError extends Error {
  constructor(
    public readonly code: number,
    text: string,
  ) {
    super(text);
    this.name = 'ApiError';
  }
}

interface RefreshedTokens {
  accessToken: string;
  refreshToken: string;
}

const baseURL = import.meta.env.VITE_API_BASE_URL ?? '/api/v1';

const client = axios.create({
  baseURL,
  timeout: 20_000,
});

client.interceptors.request.use((config) => {
  const token = tokenStorage.getAccessToken();
  if (token) {
    config.headers.set('Authorization', `Bearer ${token}`);
  }
  return config;
});

let refreshPromise: Promise<string | null> | null = null;

async function refreshAccessToken(): Promise<string | null> {
  if (!refreshPromise) {
    refreshPromise = (async () => {
      const refreshToken = tokenStorage.getRefreshToken();
      if (!refreshToken) {
        return null;
      }
      try {
        const response = await axios.post<ApiResult<RefreshedTokens>>(
          `${baseURL}/auth/refresh`,
          { refreshToken },
          { timeout: 10_000 },
        );
        if (response.data.code !== 0 || !response.data.data) {
          return null;
        }
        tokenStorage.setTokens(
          response.data.data.accessToken,
          response.data.data.refreshToken,
        );
        return response.data.data.accessToken;
      } catch {
        return null;
      } finally {
        refreshPromise = null;
      }
    })();
  }
  return refreshPromise;
}

type RetryableConfig = AxiosRequestConfig & { _retry?: boolean };

client.interceptors.response.use(
  (response) => response,
  async (error: AxiosError<ApiResult<unknown>>) => {
    const original = error.config as RetryableConfig | undefined;
    const status = error.response?.status;
    const url = original?.url ?? '';
    const isAuthEndpoint =
      url.includes('/auth/login') ||
      url.includes('/auth/register') ||
      url.includes('/auth/refresh');

    if (
      status === 401 &&
      original &&
      !original._retry &&
      !isAuthEndpoint &&
      tokenStorage.getRefreshToken()
    ) {
      original._retry = true;
      const accessToken = await refreshAccessToken();
      if (accessToken) {
        const retryConfig: AxiosRequestConfig = {
          ...original,
          headers: {
            ...(original.headers as unknown as Record<string, string>),
            Authorization: `Bearer ${accessToken}`,
          },
        };
        return client.request(retryConfig);
      }
    }

    if (status === 401 && !isAuthEndpoint) {
      tokenStorage.clear();
      message.error('登录已过期，请重新登录');
      const path = window.location.pathname;
      if (!path.startsWith('/login') && !path.startsWith('/register')) {
        // 不在独立登录页打断创作：回到公开创作台，由同页弹窗承接登录。
        // sessionStorage 中的草稿会在整页跳转后恢复。
        const authReason = path.startsWith('/works') ? 'works' : 'expired';
        window.location.replace(`/create?auth=${authReason}`);
      }
    }

    return Promise.reject(error);
  },
);

/**
 * 统一请求方法：解包 Result<T>，code !== 0 时抛出 ApiError。
 */
export async function http<T>(config: AxiosRequestConfig): Promise<T> {
  const response = await client.request<ApiResult<T>>(config);
  const body = response.data;
  if (!body || typeof body.code !== 'number') {
    return body as unknown as T;
  }
  if (body.code !== 0) {
    throw new ApiError(body.code, body.message);
  }
  return body.data;
}
