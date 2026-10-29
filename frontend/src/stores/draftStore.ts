import { create } from 'zustand';

const DRAFT_STORAGE_KEY = 'aivideo.create.draft';
const DRAFT_TTL = 24 * 60 * 60 * 1000;

export interface DraftParams {
  style: string;
  aspect: string;
  duration: number;
  resolution: string;
}

export const DEFAULT_DRAFT_PARAMS: DraftParams = {
  style: 'cinematic',
  aspect: '16:9',
  duration: 5,
  resolution: '1080p',
};

interface StoredDraft {
  prompt: string;
  params: Partial<DraftParams>;
  savedAt: number;
}

interface DraftState {
  prompt: string;
  params: DraftParams;
  setPrompt: (prompt: string) => void;
  setParams: (patch: Partial<DraftParams>) => void;
  clearPrompt: () => void;
  reset: () => void;
}

function readDraft(): { prompt: string; params: DraftParams } {
  try {
    const raw = sessionStorage.getItem(DRAFT_STORAGE_KEY);
    if (!raw) {
      return { prompt: '', params: DEFAULT_DRAFT_PARAMS };
    }
    const stored = JSON.parse(raw) as StoredDraft;
    if (!stored.savedAt || Date.now() - stored.savedAt > DRAFT_TTL) {
      sessionStorage.removeItem(DRAFT_STORAGE_KEY);
      return { prompt: '', params: DEFAULT_DRAFT_PARAMS };
    }
    return {
      prompt: typeof stored.prompt === 'string' ? stored.prompt : '',
      params: { ...DEFAULT_DRAFT_PARAMS, ...stored.params },
    };
  } catch {
    return { prompt: '', params: DEFAULT_DRAFT_PARAMS };
  }
}

function persistDraft(prompt: string, params: DraftParams): void {
  try {
    if (!prompt.trim()) {
      sessionStorage.removeItem(DRAFT_STORAGE_KEY);
      return;
    }
    const payload: StoredDraft = { prompt, params, savedAt: Date.now() };
    sessionStorage.setItem(DRAFT_STORAGE_KEY, JSON.stringify(payload));
  } catch {
    // sessionStorage 不可用时仍保留页面内状态
  }
}

const initialDraft = readDraft();

/**
 * 创作台草稿：登录弹窗、401 跳转、刷新页面都不应丢失用户输入。
 * 只存 sessionStorage，24 小时过期，避免长期保留敏感创意内容。
 */
export const useDraftStore = create<DraftState>()((set, get) => ({
  prompt: initialDraft.prompt,
  params: initialDraft.params,

  setPrompt: (prompt) => {
    set({ prompt });
    persistDraft(prompt, get().params);
  },

  setParams: (patch) => {
    const params = { ...get().params, ...patch };
    set({ params });
    persistDraft(get().prompt, params);
  },

  clearPrompt: () => {
    set({ prompt: '' });
    persistDraft('', get().params);
  },

  reset: () => {
    set({ prompt: '', params: DEFAULT_DRAFT_PARAMS });
    try {
      sessionStorage.removeItem(DRAFT_STORAGE_KEY);
    } catch {
      // 忽略存储异常
    }
  },
}));
