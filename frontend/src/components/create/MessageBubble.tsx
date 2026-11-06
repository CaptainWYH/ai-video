import { UserOutlined } from '@ant-design/icons';
import dayjs from 'dayjs';
import ReasoningTrace from '@/components/create/ReasoningTrace';
import type { ChatMessage } from '@/types/chat';

interface MessageBubbleProps {
  message: ChatMessage;
  streaming?: boolean;
}

export default function MessageBubble({ message, streaming = false }: MessageBubbleProps) {
  const isUser = message.role === 'USER';
  const content = message.content || (streaming ? '正在组织画面…' : '');

  return (
    <div className={`msg-row${isUser ? ' is-user' : ' is-assistant'}`}>
      <span className={`msg-avatar${isUser ? ' user' : ' assistant'}`} aria-hidden="true">
        {isUser ? (
          <UserOutlined />
        ) : (
          <svg viewBox="0 0 24 24" fill="none">
            <path
              d="M7 4.5h10a2.5 2.5 0 0 1 2.5 2.5v10a2.5 2.5 0 0 1-2.5 2.5H7A2.5 2.5 0 0 1 4.5 17V7A2.5 2.5 0 0 1 7 4.5Z"
              stroke="currentColor"
              strokeWidth="1.5"
            />
            <path d="m16.3 13.1 2.2 1.6-2.2 1.5v-3.1Z" fill="currentColor" />
          </svg>
        )}
      </span>

      <div className="msg-column">
        <div className="msg-meta">
          <span className="msg-author">{isUser ? '我' : 'AI 导演'}</span>
          {message.toolName ? <span className="msg-tool">{message.toolName}</span> : null}
          <time dateTime={message.createdAt}>{dayjs(message.createdAt).format('HH:mm')}</time>
        </div>

        {!isUser && message.reasoningSteps?.length ? (
          <ReasoningTrace steps={message.reasoningSteps} />
        ) : null}

        <div className={isUser ? 'msg-user-bubble' : 'msg-assistant-content'}>
          {content}
          {streaming ? <span className="streaming-cursor" aria-hidden="true" /> : null}
        </div>

        {!isUser && message.videoIntent ? (
          <div className="msg-intent">
            <span className="msg-intent-dot" aria-hidden="true" />
            分镜意图已识别 · 生成进度会显示在对话下方
          </div>
        ) : null}
      </div>
    </div>
  );
}
