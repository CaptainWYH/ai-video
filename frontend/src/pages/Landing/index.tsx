import {
  ArrowRightOutlined,
  BgColorsOutlined,
  ClockCircleOutlined,
  FolderOpenOutlined,
  MessageOutlined,
  PlayCircleFilled,
  ThunderboltOutlined,
} from '@ant-design/icons';
import { Button } from 'antd';
import { useState } from 'react';
import { Navigate, useNavigate } from 'react-router-dom';
import Brand from '@/components/common/Brand';
import ThemeToggle from '@/components/common/ThemeToggle';
import { useAuthGate } from '@/stores/authGate';
import { useDraftStore, type DraftParams } from '@/stores/draftStore';
import { tokenStorage } from '@/utils/tokenStorage';

interface Example {
  id: string;
  title: string;
  prompt: string;
  note: string;
  poster: string;
  style: string;
  aspect: string;
  duration: number;
}

const EXAMPLES: Example[] = [
  {
    id: '01',
    title: '海边日落',
    prompt:
      '夕阳贴着海平面缓慢下沉，海浪一层层拍打黑色礁石，镜头从低处向前推进，暖金色光线穿过薄雾，电影感，35mm 胶片颗粒。',
    note: '低机位推进 / 暖金色调',
    poster: 'poster-a',
    style: 'cinematic',
    aspect: '16:9',
    duration: 10,
  },
  {
    id: '02',
    title: '雨夜霓虹',
    prompt:
      '雨夜的东京小巷，霓虹招牌倒映在湿漉漉的沥青路面上，人物撑伞从画面深处走来，镜头缓慢横移，青蓝色与洋红色交错，赛博朋克。',
    note: '横移镜头 / 青蓝与洋红',
    poster: 'poster-b',
    style: 'cyberpunk',
    aspect: '21:9',
    duration: 10,
  },
  {
    id: '03',
    title: '山间晨雾',
    prompt:
      '清晨的山谷被薄雾覆盖，第一束阳光越过山脊，照亮草叶上的露水，镜头从云雾中缓缓升起，水墨质感，留白，安静。',
    note: '航拍升起 / 水墨留白',
    poster: 'poster-c',
    style: 'ink',
    aspect: '16:9',
    duration: 15,
  },
];

const CAPABILITIES = [
  {
    icon: <MessageOutlined />,
    title: '对话整理分镜',
    text: '把一句模糊的灵感拆成场景、镜头与动作，继续追问即可细化。',
  },
  {
    icon: <BgColorsOutlined />,
    title: '风格与比例',
    text: '电影感、写实、动漫、水墨与赛博朋克，横屏、竖屏或宽银幕。',
  },
  {
    icon: <ClockCircleOutlined />,
    title: '任务进度',
    text: '生成过程实时推进，离开页面也不会打断已经提交的任务。',
  },
  {
    icon: <FolderOpenOutlined />,
    title: '作品归档',
    text: '每次生成都留下记录，提示词、参数与结果集中在一个作品页。',
  },
];

export default function LandingPage() {
  const navigate = useNavigate();
  const openGate = useAuthGate((state) => state.openGate);
  const [activeExample, setActiveExample] = useState<string | null>(null);

  const handleUseExample = (example: Example) => {
    setActiveExample(example.id);
    const params: Partial<DraftParams> = {
      style: example.style,
      aspect: example.aspect,
      duration: example.duration,
    };
    useDraftStore.getState().setPrompt(example.prompt);
    useDraftStore.getState().setParams(params);
    navigate('/create');
  };

  const handleLogin = () => {
    openGate({
      reason: 'login',
      action: () => navigate('/create'),
    });
  };

  const handleWorks = () => {
    openGate({
      reason: 'works',
      action: () => navigate('/works'),
    });
  };

  if (tokenStorage.getAccessToken()) {
    return <Navigate to="/create" replace />;
  }

  return (
    <div className="landing">
      <header className="site-header">
        <Brand to="/" />
        <nav className="site-nav" aria-label="主导航">
          <a href="#workflow">创作流程</a>
          <a href="#examples">示例片场</a>
          <a href="#capabilities">能力</a>
          <button type="button" className="site-nav-link" onClick={handleWorks}>
            作品
          </button>
        </nav>
        <div className="header-actions">
          <ThemeToggle />
          <button type="button" className="btn-login" onClick={handleLogin}>
            登录
          </button>
          <Button
            type="primary"
            className="btn-start"
            onClick={() => navigate('/create')}
          >
            立即开始
            <ArrowRightOutlined />
          </Button>
        </div>
      </header>

      <main>
        <section className="landing-hero">
          <div className="landing-copy">
            <div className="eyebrow">
              <span className="eyebrow-line" />
              AI VIDEO STUDIO / NOCTURNE
            </div>
            <h1>
              一句话，
              <br />
              <em>拍成一支片。</em>
            </h1>
            <p className="landing-lede">
              写下你想看到的画面，AI 导演会把它拆成可执行的分镜：镜头、光线、运镜与节奏，
              都留在同一页里。
            </p>
            <div className="landing-actions">
              <Button
                type="primary"
                size="large"
                className="landing-primary"
                onClick={() => navigate('/create')}
              >
                立即开始创作
                <ArrowRightOutlined />
              </Button>
              <a className="landing-secondary" href="#examples">
                <PlayCircleFilled />
                看 30 秒示例
              </a>
            </div>
            <div className="landing-meta">
              <span>打开就能写</span>
              <span>分镜自动整理</span>
              <span>草稿自动保存</span>
            </div>
          </div>

          <div className="showreel-wrap">
            <div className="showreel">
              <div className="showreel-glow" aria-hidden="true" />
              <div className="showreel-top">
                <span>EXAMPLE 01</span>
                <span>35MM · ANAMORPHIC</span>
              </div>
              <div className="showreel-scene">
                <span className="showreel-sun" aria-hidden="true" />
                <span className="showreel-horizon" aria-hidden="true" />
                <span className="showreel-figure" aria-hidden="true" />
                <span className="showreel-mark" aria-hidden="true">
                  AI
                </span>
              </div>
              <div className="showreel-bottom">
                <div>
                  <span className="showreel-title">海边日落</span>
                  <span className="showreel-sub">15s · 电影感 · 16:9</span>
                </div>
                <span className="showreel-time">00:15</span>
              </div>
            </div>
            <div className="showreel-caption">
              <span>示例预览</span>
              <span>不是你的生成结果</span>
            </div>
          </div>
        </section>

        <section className="landing-section" id="workflow">
          <div className="section-heading">
            <div className="eyebrow">
              <span className="eyebrow-line" />
              01 / WORKFLOW
            </div>
            <h2>从一句话，到一支片。</h2>
            <p>把创作过程留在同一个页面，不需要在工具之间搬运上下文。</p>
          </div>
          <div className="process-grid">
            <article className="process-step">
              <span className="process-index">01</span>
              <h3>写下画面</h3>
              <p>描述主体、光线、运镜与情绪。写得具体，分镜会更接近你想要的样子。</p>
              <span className="process-note">PROMPT</span>
            </article>
            <article className="process-step">
              <span className="process-index">02</span>
              <h3>和 AI 导演对话</h3>
              <p>它会补全镜头、节奏与视觉风格。你可以继续改，直到分镜成立。</p>
              <span className="process-note">STORYBOARD</span>
            </article>
            <article className="process-step">
              <span className="process-index">03</span>
              <h3>生成并归档</h3>
              <p>确认参数后提交任务，生成进度实时推进，结果自动进入作品页。</p>
              <span className="process-note">GENERATE</span>
            </article>
          </div>
        </section>

        <section className="landing-section" id="examples">
          <div className="section-heading">
            <div className="eyebrow">
              <span className="eyebrow-line" />
              02 / EXAMPLES
            </div>
            <h2>三种画面，三种拍法。</h2>
            <p>每个示例都带完整提示词。点一下，直接把提示词带进创作台。</p>
          </div>
          <div className="examples-grid">
            {EXAMPLES.map((example) => (
              <article className="example-card" key={example.id}>
                <div className={`example-poster ${example.poster}`}>
                  <div className="example-poster-grain" aria-hidden="true" />
                  <span className="example-tag">示例 {example.id}</span>
                  <span className="example-poster-mark" aria-hidden="true">
                    {example.id}
                  </span>
                </div>
                <div className="example-body">
                  <div className="example-head">
                    <h3>{example.title}</h3>
                    <span>{example.note}</span>
                  </div>
                  <p className="example-prompt">{example.prompt}</p>
                  <div className="example-foot">
                    <span>
                      {example.aspect} · {example.duration}s
                    </span>
                    <button
                      type="button"
                      className="example-use"
                      onClick={() => handleUseExample(example)}
                    >
                      {activeExample === example.id ? '正在带入…' : '用这个提示词'}
                      <ArrowRightOutlined />
                    </button>
                  </div>
                </div>
              </article>
            ))}
          </div>
        </section>

        <section className="landing-section" id="capabilities">
          <div className="section-heading">
            <div className="eyebrow">
              <span className="eyebrow-line" />
              03 / CAPABILITIES
            </div>
            <h2>创作需要的，留在同一张桌上。</h2>
            <p>对话、参数、任务与作品记录共享同一份上下文。</p>
          </div>
          <div className="capability-grid">
            {CAPABILITIES.map((item) => (
              <article className="capability-item" key={item.title}>
                <span className="capability-icon">{item.icon}</span>
                <h3>{item.title}</h3>
                <p>{item.text}</p>
              </article>
            ))}
          </div>
        </section>

        <section className="cta-band">
          <div className="cta-band-glow" aria-hidden="true" />
          <div>
            <div className="eyebrow">
              <span className="eyebrow-line" />
              ENTER THE STUDIO
            </div>
            <h2>现在就把第一句话，交给画面。</h2>
            <p>进入创作台，写下你的第一个镜头。分镜定稿后，一键生成成片。</p>
          </div>
          <Button
            type="primary"
            size="large"
            className="landing-primary"
            onClick={() => navigate('/create')}
          >
            进入创作台
            <ArrowRightOutlined />
          </Button>
        </section>

        <footer className="landing-footer">
          <Brand to="/" />
          <p>AI Video Studio · 把灵感拍成片的视频创作工作台</p>
          <span>
            <ThunderboltOutlined /> 对话 · 分镜 · 生成
          </span>
        </footer>
      </main>
    </div>
  );
}
