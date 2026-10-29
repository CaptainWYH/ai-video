import { http } from '@/api/request';
import type { LoginPayload, RegisterPayload, TokenResponse, UserInfo } from '@/types/auth';

export const authApi = {
  login(payload: LoginPayload) {
    return http<TokenResponse>({
      url: '/auth/login',
      method: 'POST',
      data: payload,
    });
  },

  register(payload: RegisterPayload) {
    return http<TokenResponse>({
      url: '/auth/register',
      method: 'POST',
      data: payload,
    });
  },

  logout(refreshToken?: string) {
    return http<void>({
      url: '/auth/logout',
      method: 'POST',
      data: refreshToken ? { refreshToken } : {},
    });
  },

  me() {
    return http<UserInfo>({
      url: '/auth/me',
      method: 'GET',
    });
  },
};
