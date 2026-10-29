import { create } from 'zustand';

export type ThemeMode = 'light' | 'dark';

export const THEME_STORAGE_KEY = 'aivideo.theme';

const THEME_COLOR: Record<ThemeMode, string> = {
  light: '#FAF7F2',
  dark: '#14110E',
};

function readStored(): ThemeMode | null {
  try {
    const saved = localStorage.getItem(THEME_STORAGE_KEY);
    return saved === 'light' || saved === 'dark' ? saved : null;
  } catch {
    return null;
  }
}

function systemMode(): ThemeMode {
  if (typeof window === 'undefined' || !window.matchMedia) {
    return 'light';
  }
  return window.matchMedia('(prefers-color-scheme: dark)').matches ? 'dark' : 'light';
}

function applyTheme(mode: ThemeMode) {
  const root = document.documentElement;
  root.dataset.theme = mode;
  root.style.colorScheme = mode;
  const meta = document.querySelector('meta[name="theme-color"]');
  if (meta) {
    meta.setAttribute('content', THEME_COLOR[mode]);
  }
}

const initialMode = readStored() ?? systemMode();
applyTheme(initialMode);

interface ThemeState {
  mode: ThemeMode;
  setMode: (mode: ThemeMode) => void;
  toggle: () => void;
}

export const useThemeStore = create<ThemeState>((set, get) => ({
  mode: initialMode,
  setMode: (mode) => {
    try {
      localStorage.setItem(THEME_STORAGE_KEY, mode);
    } catch {
      // 隐私模式下存储不可用时，仅当前会话生效
    }
    applyTheme(mode);
    set({ mode });
  },
  toggle: () => {
    get().setMode(get().mode === 'dark' ? 'light' : 'dark');
  },
}));

// 用户尚未手动选择时，跟随系统外观变化
if (typeof window !== 'undefined' && window.matchMedia) {
  const media = window.matchMedia('(prefers-color-scheme: dark)');
  media.addEventListener('change', (event) => {
    if (readStored()) {
      return;
    }
    const mode: ThemeMode = event.matches ? 'dark' : 'light';
    applyTheme(mode);
    useThemeStore.setState({ mode });
  });
}
