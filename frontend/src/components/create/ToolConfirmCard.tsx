import {
  CheckCircleOutlined,
  CloseCircleOutlined,
  ExclamationCircleOutlined,
  StopOutlined,
} from '@ant-design/icons';
import ReasoningTrace from '@/components/create/ReasoningTrace';
import type { ChatMessage, ConfirmStatus } from '@/types/chat';

interface ToolConfirmCardProps {
  message: ChatMessage;
  busy?: boolean;
  onConfirm: (approved: boolean) => void;
}

const STATUS_LABEL: Record<ConfirmStatus, string> = {
  PENDING: '等待你确认后才会执行该工具',
  APPROVED: '已允许执行，工具返回结果已写入对话',
  DENIED: '已拒绝执行，本次工具调用未执行',
  EXPIRED: '确认已失效，请重新发送消息',
  FAILED: '工具执行失败，请重试或重新发送消息',
};

function formatValue(value: unknown): string {
  if (value === null || value === undefined) {
    return '—';
  }
  if (typeof value === 'object') {
    return JSON.stringify(value);
  }
  return String(value);
}

export default function ToolConfirmCard({
  message,
  busy = false,
  onConfirm,
}: ToolConfirmCardProps) {
  const status = message.confirmStatus ?? 'PENDING';
  const pending = status === 'PENDING';
  const toolCalls = message.toolCalls ?? [];
  const primary = toolCalls[0];
  const title =
    toolCalls.length > 1
      ? `${toolCalls.length} 个工具调用等待确认`
      : `AI 请求调用 ${primary?.name ?? message.toolName ?? '工具'}`;

  return (
    <>
      {message.reasoningSteps?.length ? <ReasoningTrace steps={message.reasoningSteps} /> : null}
      <article
        className={`gen-card tool-confirm-card${status === 'APPROVED' ? ' is-success' : ''}${
          status === 'DENIED' || status === 'FAILED' || status === 'EXPIRED' ? ' is-failed' : ''
        }`}
        aria-live="polite"
      >
        <div className="gen-body">
          <div className="gen-eyebrow">TOOL CONFIRMATION</div>
          <h3 className="gen-title">{title}</h3>

          <div className="gen-meta">
            <span>{primary?.name ?? 'tool'}</span>
            <span>{toolCalls.length} 个调用</span>
          </div>

          <div className="tool-confirm-params">
            {toolCalls.map((toolCall) => (
              <div className="tool-confirm-call" key={toolCall.id}>
                <div className="tool-confirm-call-name">{toolCall.name}</div>
                {Object.entries(toolCall.input).map(([key, value]) => (
                  <div className="tool-confirm-param" key={key}>
                    <span className="tool-confirm-param-key">{key}</span>
                    <span className="tool-confirm-param-value">{formatValue(value)}</span>
                  </div>
                ))}
              </div>
            ))}
          </div>

          <div className={`tool-confirm-status is-${status.toLowerCase()}`}>
            {pending ? (
              <ExclamationCircleOutlined />
            ) : status === 'APPROVED' ? (
              <CheckCircleOutlined />
            ) : status === 'DENIED' ? (
              <CloseCircleOutlined />
            ) : (
              <StopOutlined />
            )}
            <span>{STATUS_LABEL[status]}</span>
          </div>

          {pending ? (
            <div className="gen-actions">
              <button
                type="button"
                className="gen-action is-primary"
                disabled={busy}
                onClick={() => onConfirm(true)}
              >
                {busy ? '处理中…' : '允许执行'}
              </button>
              <button
                type="button"
                className="gen-action"
                disabled={busy}
                onClick={() => onConfirm(false)}
              >
                拒绝
              </button>
            </div>
          ) : null}
        </div>
      </article>
    </>
  );
}
