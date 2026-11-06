import { BranchesOutlined, CheckOutlined, ToolOutlined } from '@ant-design/icons';
import type { ChatMessage } from '@/types/chat';

interface PromptProposalCardProps {
  message: ChatMessage;
  busy: boolean;
  onConfirm: (useOptimizedPrompt: boolean) => void;
}

export default function PromptProposalCard({
  message,
  busy,
  onConfirm,
}: PromptProposalCardProps) {
  const proposal = message.promptProposal;
  if (!proposal) {
    return null;
  }
  const decided = message.promptDecisionStatus === 'SELECTED';

  return (
    <article className="prompt-proposal-card" aria-live="polite">
      <header className="prompt-proposal-head">
        <span className="prompt-proposal-icon" aria-hidden="true">
          <BranchesOutlined />
        </span>
        <div>
          <div className="prompt-proposal-eyebrow">AI PROMPT REVIEW</div>
          <h3>我整理了一版更完整的提示词</h3>
        </div>
      </header>

      <div className="prompt-proposal-trace">
        <ToolOutlined />
        <span>调用工具</span>
        <code>{proposal.toolName}</code>
      </div>

      <ol className="prompt-analysis-steps">
        {proposal.analysisSteps.map((step, index) => (
          <li key={`${index}-${step}`}>
            <span>{String(index + 1).padStart(2, '0')}</span>
            {step}
          </li>
        ))}
      </ol>

      <div className="prompt-version-grid">
        <section>
          <span className="prompt-version-label">原始提示词</span>
          <p>{proposal.originalPrompt}</p>
        </section>
        <section className="is-optimized">
          <span className="prompt-version-label">优化后提示词</span>
          <p>{proposal.optimizedPrompt}</p>
        </section>
      </div>

      {decided ? (
        <div className="prompt-proposal-result">
          <CheckOutlined />
          已使用{message.useOptimizedPrompt ? '优化版' : '原始版'}提示词
        </div>
      ) : (
        <div className="prompt-proposal-actions">
          <button
            type="button"
            className="prompt-proposal-button is-primary"
            disabled={busy}
            onClick={() => onConfirm(true)}
          >
            使用优化版并生成
          </button>
          <button
            type="button"
            className="prompt-proposal-button"
            disabled={busy}
            onClick={() => onConfirm(false)}
          >
            使用原版并生成
          </button>
        </div>
      )}
    </article>
  );
}
