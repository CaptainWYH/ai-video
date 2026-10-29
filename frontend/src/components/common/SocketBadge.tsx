interface SocketBadgeProps {
  connected: boolean;
}

export default function SocketBadge({ connected }: SocketBadgeProps) {
  return (
    <span className={`socket-badge${connected ? ' is-online' : ''}`}>
      <span className="socket-dot" aria-hidden="true" />
      {connected ? '实时同步' : '离线兜底'}
    </span>
  );
}
