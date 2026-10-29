import { ArrowLeftOutlined } from '@ant-design/icons';
import { Link, Navigate, useNavigate } from 'react-router-dom';
import AuthForm from '@/components/auth/AuthForm';
import Brand from '@/components/common/Brand';
import ThemeToggle from '@/components/common/ThemeToggle';
import { tokenStorage } from '@/utils/tokenStorage';

export default function RegisterPage() {
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
            NEW CREATOR
          </div>
          <h1>
            从第一句话，
            <br />
            <em>开始拍。</em>
          </h1>
          <p>
            注册后即可保存对话、继续未完成的草稿，并把每次生成收进自己的作品页。
          </p>
          <div className="auth-steps">
            <div className="auth-step">
              <span>01</span>
              <p>写下画面与情绪</p>
            </div>
            <div className="auth-step">
              <span>02</span>
              <p>和 AI 导演一起改分镜</p>
            </div>
            <div className="auth-step">
              <span>03</span>
              <p>生成、回看、再创作</p>
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
              CREATE ACCOUNT
            </div>
            <h2>创建你的创作账号</h2>
            <p>只需要一个用户名和密码，就可以开始第一支片。</p>
          </div>

          <AuthForm mode="register" onSuccess={() => navigate('/create', { replace: true })} />

          <div className="auth-switch">
            已经有账号？
            <Link to="/login">直接登录</Link>
          </div>

          <div className="auth-note">注册即表示你了解当前视频生成能力仍为占位演示。</div>
        </div>
      </section>
    </div>
  );
}
