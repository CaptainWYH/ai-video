import { ArrowRightOutlined, LoadingOutlined } from '@ant-design/icons';
import { Drawer, Spin, message } from 'antd';
import { Fragment, useCallback, useEffect, useRef, useState } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import GenerationCard from '@/components/create/GenerationCard';
import MessageBubble from '@/components/create/MessageBubble';
import PromptComposer from '@/components/create/PromptComposer';
import PromptProposalCard from '@/components/create/PromptProposalCard';
import ReasoningTrace from '@/components/create/ReasoningTrace';
import ToolConfirmCard from '@/components/create/ToolConfirmCard';
import WorkspaceSidebar from '@/components/create/WorkspaceSidebar';
import WorkspaceTopBar from '@/components/create/WorkspaceTopBar';
import { useChatSocket } from '@/hooks/useChatSocket';
import { useAuthGate } from '@/stores/authGate';
import { useAuthStore } from '@/stores/authStore';
import { useChatStore } from '@/stores/chatStore';
import { useDraftStore, type DraftParams } from '@/stores/draftStore';
import { videoApi } from '@/api/video';
import { getErrorMessage } from '@/utils/error';
import { tokenStorage } from '@/utils/tokenStorage';
import type { ChatMessage } from '@/types/chat';
import type { VideoTask } from '@/types/video';

const SUGGESTIONS = [
  '海边日落，镜头从低处缓慢推进，暖金色调，电影感',
  '雨夜霓虹街头，人物撑伞走来，横移镜头，赛博朋克',
  '清晨山谷的薄雾，第一束阳光越过山脊，水墨质感',
];

export default function CreatePage() {
  const navigate = useNavigate();
  const [searchParams, setSearchParams] = useSearchParams();
  const user = useAuthStore((state) => state.user);
  const fetchMe = useAuthStore((state) => state.fetchMe);
  const logout = useAuthStore((state) => state.logout);
  const openGate = useAuthGate((state) => state.openGate);

  const sessions = useChatStore((state) => state.sessions);
  const activeSessionId = useChatStore((state) => state.activeSessionId);
  const messages = useChatStore((state) => state.messages);
  const loadingSessions = useChatStore((state) => state.loadingSessions);
  const loadingMessages = useChatStore((state) => state.loadingMessages);
  const streaming = useChatStore((state) => state.streaming);
  const liveTrace = useChatStore((state) => state.liveTrace);
  const socketConnected = useChatStore((state) => state.socketConnected);
  const confirmingMessageId = useChatStore((state) => state.confirmingMessageId);
  const decidingPromptMessageId = useChatStore((state) => state.decidingPromptMessageId);
  const handleVideoProgress = useChatStore((state) => state.handleVideoProgress);
  const loadSessions = useChatStore((state) => state.loadSessions);
  const createSession = useChatStore((state) => state.createSession);
  const selectSession = useChatStore((state) => state.selectSession);
  const sendMessage = useChatStore((state) => state.sendMessage);
  const confirmToolCall = useChatStore((state) => state.confirmToolCall);
  const decidePrompt = useChatStore((state) => state.decidePrompt);
  const resetChat = useChatStore((state) => state.reset);

  const prompt = useDraftStore((state) => state.prompt);
  const params = useDraftStore((state) => state.params);
  const setPrompt = useDraftStore((state) => state.setPrompt);
  const setParams = useDraftStore((state) => state.setParams);
  const clearPrompt = useDraftStore((state) => state.clearPrompt);

  const [drawerOpen, setDrawerOpen] = useState(false);
  const [creating, setCreating] = useState(false);
  const [sending, setSending] = useState(false);
  const [composerError, setComposerError] = useState<string | null>(null);
  const bottomRef = useRef<HTMLDivElement>(null);

  const loggedIn = Boolean(tokenStorage.getAccessToken());
  useChatSocket(loggedIn);

  useEffect(() => {
    if (!loggedIn) {
      resetChat();
      return;
    }
    if (!user) {
      void fetchMe();
    }
  }, [loggedIn, user, fetchMe, resetChat]);

  useEffect(() => {
    if (!loggedIn) {
      return;
    }
    loadSessions().catch((error) => message.error(getErrorMessage(error)));
  }, [loggedIn, loadSessions]);

  useEffect(() => {
    const auth = searchParams.get('auth');
    if (!auth) {
      return;
    }
    if (!tokenStorage.getAccessToken()) {
      const isWorks = auth === 'works';
      openGate({
        reason: isWorks ? 'works' : 'expired',
        action: isWorks ? () => navigate('/works', { replace: true }) : undefined,
      });
    }
    const next = new URLSearchParams(searchParams);
    next.delete('auth');
    setSearchParams(next, { replace: true });
  }, [searchParams, openGate, navigate, setSearchParams]);

  useEffect(() => {
    const frame = requestAnimationFrame(() => {
      bottomRef.current?.scrollIntoView({ behavior: 'smooth', block: 'end' });
    });
    return () => cancelAnimationFrame(frame);
  }, [messages, streaming?.content, liveTrace]);

  const activeVideoTasks = messages
    .map((item) => item.videoTask)
    .filter((task): task is VideoTask => Boolean(task))
    .filter((task) =>
      ['PENDING', 'SUBMITTING', 'QUEUED', 'RUNNING', 'DOWNLOADING'].includes(task.status),
    );
  const activeVideoTaskKey = activeVideoTasks.map((task) => task.id).join(',');

  useEffect(() => {
    if (!activeVideoTaskKey) {
      return undefined;
    }
    const taskIds = activeVideoTaskKey.split(',').map(Number);
    const refresh = () => {
      void Promise.all(taskIds.map((taskId) => videoApi.getTask(taskId).catch(() => null))).then(
        (tasks) => {
          tasks
            .filter((task): task is VideoTask => Boolean(task))
            .forEach((task) => {
              handleVideoProgress({
                taskId: task.id,
                sessionId: activeSessionId,
                status: task.status,
                progress: task.progress,
                resultUrl: task.resultUrl,
                usage: task.usage,
              });
            });
        },
      );
    };
    refresh();
    const timer = window.setInterval(refresh, 3000);
    return () => window.clearInterval(timer);
  }, [activeVideoTaskKey, activeSessionId, handleVideoProgress]);

  const generateNow = useCallback(
    async (text: string, nextParams: DraftParams) => {
      setSending(true);
      setComposerError(null);
      try {
        const reply = await sendMessage(text, { ...nextParams });
        if (!reply) {
          return;
        }
        clearPrompt();
        if (reply.messageType === 'TOOL_CONFIRM') {
          // 等待用户在消息流内完成确认，不再自动创建视频任务。
          return;
        }
      } catch (error) {
        setComposerError(getErrorMessage(error));
      } finally {
        setSending(false);
      }
    },
    [sendMessage, clearPrompt],
  );

  const handleConfirm = useCallback(
    async (messageId: number, approved: boolean) => {
      try {
        await confirmToolCall(messageId, approved);
      } catch (error) {
        message.error(getErrorMessage(error));
      }
    },
    [confirmToolCall],
  );

  const handlePromptDecision = useCallback(
    async (messageId: number, useOptimizedPrompt: boolean) => {
      try {
        await decidePrompt(messageId, useOptimizedPrompt);
        message.success(useOptimizedPrompt ? '已采用优化版提示词' : '已采用原始提示词');
      } catch (error) {
        message.error(getErrorMessage(error));
      }
    },
    [decidePrompt],
  );

  const handleGenerate = useCallback(() => {
    const text = prompt.trim();
    if (!text || sending) {
      return;
    }
    if (!tokenStorage.getAccessToken()) {
      openGate({
        reason: 'generate',
        prompt: text,
        params,
        action: () => generateNow(text, params),
      });
      return;
    }
    void generateNow(text, params);
  }, [prompt, sending, params, openGate, generateNow]);

  const handleNew = async () => {
    setDrawerOpen(false);
    setComposerError(null);
    if (!loggedIn) {
      clearPrompt();
      return;
    }
    setCreating(true);
    try {
      await createSession();
      clearPrompt();
    } catch (error) {
      message.error(getErrorMessage(error));
    } finally {
      setCreating(false);
    }
  };

  const handleSelect = async (sessionId: number) => {
    setDrawerOpen(false);
    if (sessionId === activeSessionId) {
      return;
    }
    setComposerError(null);
    try {
      await selectSession(sessionId);
    } catch (error) {
      message.error(getErrorMessage(error));
    }
  };

  const handleWorks = () => {
    if (!loggedIn) {
      openGate({ reason: 'works', action: () => navigate('/works') });
      return;
    }
    navigate('/works');
  };

  const handleLogout = async () => {
    await logout();
    resetChat();
    message.success('已退出登录');
    navigate('/', { replace: true });
  };

  const activeSession = sessions.find((session) => session.id === activeSessionId) ?? null;
  const displayName = user?.nickname || user?.username || '创作者';
  const title = activeSession?.title || '新的创作';
  const subtitle = loggedIn
    ? activeSession
      ? `${messages.length} 条消息 · 继续打磨这支片`
      : '写下第一句话，AI 导演会帮你拆解分镜'
    : '游客模式 · 草稿暂存在本机';

  const streamingMessage: ChatMessage | null = streaming
    ? {
        id: -2,
        sessionId: streaming.sessionId,
        role: 'ASSISTANT',
        content: streaming.content,
        messageType: 'TEXT',
        videoIntent: false,
        createdAt: new Date().toISOString(),
      }
    : null;

  const showEmpty = !loadingMessages && messages.length === 0 && !streamingMessage;

  const retryVideo = async (task: VideoTask) => {
    try {
      await videoApi.retryTask(task.id);
      if (activeSessionId) {
        await selectSession(activeSessionId);
      }
    } catch (error) {
      message.error(getErrorMessage(error));
    }
  };

  const cancelVideo = async (task: VideoTask) => {
    try {
      const updated = await videoApi.cancelTask(task.id);
      handleVideoProgress({
        taskId: updated.id,
        sessionId: updated.id ? activeSessionId : null,
        status: updated.status,
        progress: updated.progress,
        resultUrl: updated.resultUrl,
        usage: updated.usage,
      });
      message.success('已取消视频任务');
    } catch (error) {
      message.error(getErrorMessage(error));
    }
  };

  return (
    <div className="create-shell">
      <aside className="create-sidebar">
        <WorkspaceSidebar
          guest={!loggedIn}
          user={user}
          sessions={sessions}
          activeSessionId={activeSessionId}
          loading={loadingSessions}
          creating={creating}
          onNew={handleNew}
          onSelect={handleSelect}
          onLogin={() => openGate({ reason: 'login' })}
          onLogout={handleLogout}
          onWorks={handleWorks}
        />
      </aside>

      <section className="create-main">
        <WorkspaceTopBar
          title={title}
          subtitle={subtitle}
          socketConnected={socketConnected}
          guest={!loggedIn}
          user={user}
          onMenu={() => setDrawerOpen(true)}
          onLogin={() => openGate({ reason: 'login' })}
          onLogout={handleLogout}
          onWorks={handleWorks}
        />

        <div className="create-thread">
          <div className="create-thread-inner">
            {loadingMessages ? (
              <div className="create-loading">
                <Spin indicator={<LoadingOutlined spin />} />
                <span>正在载入对话…</span>
              </div>
            ) : showEmpty ? (
              <div className="create-empty">
                <div className="create-empty-eyebrow">
                  <span className="eyebrow-line" />
                  {loggedIn ? 'NEW TAKE' : 'GUEST TAKE'}
                </div>
                <h1>
                  今天想拍什么？
                  <br />
                  <em>先说一个画面。</em>
                </h1>
                <p>
                  描述主体、光线、运镜和情绪。AI 导演会把它整理成可执行的分镜，
                  你确认后再决定是否生成。
                </p>
                <div className="create-suggestions">
                  {SUGGESTIONS.map((suggestion) => (
                    <button
                      type="button"
                      key={suggestion}
                      className="create-suggestion"
                      onClick={() => setPrompt(suggestion)}
                    >
                      <span>{suggestion}</span>
                      <ArrowRightOutlined />
                    </button>
                  ))}
                </div>
              </div>
            ) : (
              <div className="thread-messages">
                {messages.map((item) => (
                  <Fragment key={item.id}>
                    {item.messageType === 'PROMPT_PROPOSAL' ? (
                      <PromptProposalCard
                        message={item}
                        busy={decidingPromptMessageId === item.id}
                        onConfirm={(useOptimizedPrompt) =>
                          void handlePromptDecision(item.id, useOptimizedPrompt)
                        }
                      />
                    ) : item.messageType === 'TOOL_CONFIRM' ? (
                      <ToolConfirmCard
                        message={item}
                        busy={confirmingMessageId === item.id}
                        onConfirm={(approved) => void handleConfirm(item.id, approved)}
                      />
                    ) : (
                      <MessageBubble message={item} />
                    )}
                    {item.videoTask ? (
                      <GenerationCard
                        prompt={item.videoTask.prompt}
                        params={item.videoTask.params}
                        task={item.videoTask}
                        error={item.videoTask.errorMessage}
                        onRetry={() => void retryVideo(item.videoTask as VideoTask)}
                        onCancel={() => void cancelVideo(item.videoTask as VideoTask)}
                        onWorks={handleWorks}
                      />
                    ) : null}
                  </Fragment>
                ))}
                {liveTrace && liveTrace.steps.length > 0 ? (
                  <div className="trace-live-row">
                    <ReasoningTrace steps={liveTrace.steps} live />
                  </div>
                ) : null}
                {streamingMessage ? <MessageBubble message={streamingMessage} streaming /> : null}
                <div ref={bottomRef} />
              </div>
            )}
          </div>
        </div>

        <div className="create-composer-wrap">
          <div className="create-composer-inner">
            <PromptComposer
              value={prompt}
              params={params}
              guest={!loggedIn}
              loading={sending}
              error={composerError}
              onChange={(value) => {
                setPrompt(value);
                if (composerError) {
                  setComposerError(null);
                }
              }}
              onParamsChange={setParams}
              onGenerate={handleGenerate}
            />
          </div>
        </div>
      </section>

      <Drawer
        open={drawerOpen}
        onClose={() => setDrawerOpen(false)}
        placement="left"
        width={300}
        className="create-drawer"
        closable={false}
      >
        <WorkspaceSidebar
          guest={!loggedIn}
          user={user}
          sessions={sessions}
          activeSessionId={activeSessionId}
          loading={loadingSessions}
          creating={creating}
          onNew={handleNew}
          onSelect={handleSelect}
          onLogin={() => {
            setDrawerOpen(false);
            openGate({ reason: 'login' });
          }}
          onLogout={handleLogout}
          onWorks={handleWorks}
        />
      </Drawer>

      <span className="sr-only">当前用户：{displayName}</span>
    </div>
  );
}
