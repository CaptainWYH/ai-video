import { Modal } from 'antd';
import Brand from '@/components/common/Brand';
import AuthForm from '@/components/auth/AuthForm';
import { useAuthGate } from '@/stores/authGate';

const REASON_COPY = {
  generate: {
    eyebrow: 'CONTINUE CREATING',
    title: '登录后继续生成',
    description: '创意已经替你保留。登录后会自动继续生成这支视频。',
  },
  works: {
    eyebrow: 'YOUR WORK',
    title: '登录后查看作品',
    description: '作品记录与生成进度都保存在你的账号下。',
  },
  expired: {
    eyebrow: 'SESSION EXPIRED',
    title: '登录状态已过期',
    description: '重新登录即可继续，当前草稿不会丢失。',
  },
  login: {
    eyebrow: 'WELCOME BACK',
    title: '登录 AI Video Studio',
    description: '回到你的创作台，继续把想法变成画面。',
  },
} as const;

export default function AuthModal() {
  const open = useAuthGate((state) => state.open);
  const mode = useAuthGate((state) => state.mode);
  const reason = useAuthGate((state) => state.reason);
  const pendingPrompt = useAuthGate((state) => state.pendingPrompt);
  const closeGate = useAuthGate((state) => state.closeGate);
  const setMode = useAuthGate((state) => state.setMode);
  const completeGate = useAuthGate((state) => state.completeGate);

  const copy = REASON_COPY[reason ?? 'login'];

  return (
    <Modal
      open={open}
      footer={null}
      width={470}
      centered
      destroyOnClose
      className="auth-modal"
      onCancel={closeGate}
    >
      <div className="auth-modal-body">
        <div className="auth-modal-top">
          <Brand to="/" compact />
          <span className="auth-modal-kicker">{copy.eyebrow}</span>
        </div>

        <div className="auth-modal-heading">
          <h2>{copy.title}</h2>
          <p>{copy.description}</p>
        </div>

        {pendingPrompt ? (
          <div className="auth-draft">
            <span className="auth-draft-label">待生成提示词</span>
            <p>{pendingPrompt}</p>
          </div>
        ) : null}

        <div className="auth-modal-tabs" role="tablist" aria-label="登录或注册">
          <button
            type="button"
            role="tab"
            aria-selected={mode === 'login'}
            className={mode === 'login' ? 'is-active' : ''}
            onClick={() => setMode('login')}
          >
            登录
          </button>
          <button
            type="button"
            role="tab"
            aria-selected={mode === 'register'}
            className={mode === 'register' ? 'is-active' : ''}
            onClick={() => setMode('register')}
          >
            注册
          </button>
        </div>

        <AuthForm key={mode} mode={mode} onSuccess={completeGate} />

        <p className="auth-modal-note">
          视频生成能力正在开发中，当前展示为示例结果。
        </p>
      </div>
    </Modal>
  );
}
