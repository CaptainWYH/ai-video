import { ArrowLeftOutlined } from '@ant-design/icons';
import { Link, Navigate, useNavigate } from 'react-router-dom';
import AuthForm from '@/components/auth/AuthForm';
import Brand from '@/components/common/Brand';
import ThemeToggle from '@/components/common/ThemeToggle';
import { tokenStorage } from '@/utils/tokenStorage';

export default function LoginPage() {
  const navigate = useNavigate();

  if (tokenStorage.getAccessToken()) {
    return <Navigate to="/create" replace />;
  }

  return (
    <div className="auth-page">
      <aside className="auth-aside">
        <div className="auth-aside-glow" aria-hidden="true" />
        <Brand to="/" />
        <div className="auth-aside-copy">
          <div className="eyebrow">
            <span className="eyebrow-line" />
            WELCOME BACK
          </div>
          <h1>
            把灵感，
            <br />
            <em>变成镜头。</em>
          </h1>
          <p>
            从一句创意到分镜脚本，再到视频任务。用一个工作台承载你的 AI 视频创作流程。
          </p>
          <div className="auth-steps">
            <div className="auth-step">
              <span>01</span>
              <p>对话梳理创意与剧本</p>
            </div>
            <div className="auth-step">
              <span>02</span>
              <p>AI 导演拆解分镜与镜头</p>
            </div>
            <div className="auth-step">
              <span>03</span>
              <p>提交生成并归档作品</p>
            </div>
          </div>
        </div>
        <div className="auth-aside-note">NOCTURNE / AI VIDEO STUDIO</div>
      </aside>

      <section className="auth-panel">
        <div className="auth-panel-top">
          <Link to="/" className="auth-back">
            <ArrowLeftOutlined />
            返回首页
          </Link>
          <ThemeToggle />
        </div>
        <div className="auth-card">
          <div className="auth-card-head">
            <div className="eyebrow">
              <span className="eyebrow-line" />
              SIGN IN
            </div>
            <h2>继续你的创作</h2>
            <p>登录后，草稿、对话记录与生成任务都会回到原来的位置。</p>
          </div>

          <AuthForm mode="login" onSuccess={() => navigate('/create', { replace: true })} />

          <div className="auth-switch">
            还没有账号？
            <Link to="/register">创建一个</Link>
          </div>

          <div className="auth-note">
            视频生成能力正在开发中，当前展示为示例结果。
          </div>
        </div>
      </section>
    </div>
  );
}
