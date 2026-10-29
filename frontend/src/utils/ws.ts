/**
 * WebSocket 地址。未配置 VITE_WS_URL 时走当前域名的 /ws/chat（Vite dev server 已代理）。
 */
export function getWsUrl(): string {
  const configured = import.meta.env.VITE_WS_URL;
  if (configured) {
    return configured;
  }
  const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
  return `${protocol}//${window.location.host}/ws/chat`;
}
