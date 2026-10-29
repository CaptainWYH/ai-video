import { create } from 'zustand';
import { authApi } from '@/api/auth';
import { tokenStorage } from '@/utils/tokenStorage';
import type { LoginPayload, RegisterPayload, UserInfo } from '@/types/auth';

const USER_STORAGE_KEY = 'aivideo.auth.user';

function readStoredUser(): UserInfo | null {
  try {
    const raw = localStorage.getItem(USER_STORAGE_KEY);
    return raw ? (JSON.parse(raw) as UserInfo) : null;
  } catch {
    return null;
  }
}

function persistUser(user: UserInfo | null): void {
  if (user) {
    localStorage.setItem(USER_STORAGE_KEY, JSON.stringify(user));
  } else {
    localStorage.removeItem(USER_STORAGE_KEY);
  }
}

interface AuthState {
  user: UserInfo | null;
  loading: boolean;
  login: (payload: LoginPayload) => Promise<void>;
  register: (payload: RegisterPayload) => Promise<void>;
  logout: () => Promise<void>;
  fetchMe: () => Promise<void>;
  setUser: (user: UserInfo | null) => void;
}

export const useAuthStore = create<AuthState>()((set) => ({
  user: readStoredUser(),
  loading: false,

  login: async (payload) => {
    set({ loading: true });
    try {
      const result = await authApi.login(payload);
      tokenStorage.setTokens(result.accessToken, result.refreshToken);
      persistUser(result.user);
      set({ user: result.user, loading: false });
    } catch (error) {
      set({ loading: false });
      throw error;
    }
  },

  register: async (payload) => {
    set({ loading: true });
    try {
      const result = await authApi.register(payload);
      tokenStorage.setTokens(result.accessToken, result.refreshToken);
      persistUser(result.user);
      set({ user: result.user, loading: false });
    } catch (error) {
      set({ loading: false });
      throw error;
    }
  },

  logout: async () => {
    const refreshToken = tokenStorage.getRefreshToken();
    try {
      if (tokenStorage.getAccessToken()) {
        await authApi.logout(refreshToken ?? undefined);
      }
    } catch {
      // 登出接口失败不阻塞本地清理
    } finally {
      tokenStorage.clear();
      persistUser(null);
      set({ user: null });
    }
  },

  fetchMe: async () => {
    if (!tokenStorage.getAccessToken()) {
      return;
    }
    try {
      const user = await authApi.me();
      persistUser(user);
      set({ user });
    } catch {
      // 401 由 Axios 拦截器统一清理并跳转
    }
  },

  setUser: (user) => {
    persistUser(user);
    set({ user });
  },
}));

export function isAuthenticated(): boolean {
  return Boolean(tokenStorage.getAccessToken());
}
