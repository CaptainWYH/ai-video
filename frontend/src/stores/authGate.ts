import { create } from 'zustand';
import type { DraftParams } from '@/stores/draftStore';

export type AuthGateReason = 'generate' | 'works' | 'expired' | 'login';
export type AuthGateMode = 'login' | 'register';

interface OpenGatePayload {
  reason: AuthGateReason;
  prompt?: string;
  params?: DraftParams;
  action?: () => void | Promise<void>;
}

interface AuthGateState {
  open: boolean;
  mode: AuthGateMode;
  reason: AuthGateReason | null;
  pendingPrompt: string | null;
  pendingParams: DraftParams | null;
  pendingAction: (() => void | Promise<void>) | null;

  openGate: (payload: OpenGatePayload) => void;
  closeGate: () => void;
  setMode: (mode: AuthGateMode) => void;
  completeGate: () => Promise<void>;
}

/**
 * 同页登录门禁。登录成功后由 completeGate 执行挂起的动作，
 * 例如继续生成视频或跳转作品页，避免用户重新输入。
 */
export const useAuthGate = create<AuthGateState>()((set, get) => ({
  open: false,
  mode: 'login',
  reason: null,
  pendingPrompt: null,
  pendingParams: null,
  pendingAction: null,

  openGate: ({ reason, prompt, params, action }) => {
    set({
      open: true,
      mode: 'login',
      reason,
      pendingPrompt: prompt?.trim() ? prompt : null,
      pendingParams: params ?? null,
      pendingAction: action ?? null,
    });
  },

  closeGate: () => {
    set({
      open: false,
      reason: null,
      pendingPrompt: null,
      pendingParams: null,
      pendingAction: null,
    });
  },

  setMode: (mode) => set({ mode }),

  completeGate: async () => {
    const action = get().pendingAction;
    set({
      open: false,
      reason: null,
      pendingPrompt: null,
      pendingParams: null,
      pendingAction: null,
    });
    if (action) {
      await action();
    }
  },
}));
