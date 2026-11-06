# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 项目定位

AI Video Studio 的 PC 端 Monorepo，主线为「对话 → 提示词优化 → 用户选择 → 视频任务」。已接入 DeepSeek、Seedance 异步视频生成、轮询/回调状态同步、MinIO 转存、聊天状态回传和 token 消耗统计。

始终用中文回复。技术名词、代码标识、命令保持原样。

## 开始任务时

本文件是每次会话自动加载的主规范。动手前按任务范围补充阅读：

- 涉及模块划分、认证、对话、WebSocket、视频任务流程：先读 `docs/architecture.md` 的相关章节。
- 涉及接口、字段、错误码、分页：先读 `docs/api-convention.md`。
- 涉及环境变量、启动方式、Profile、Git：先读 `docs/dev-guide.md`。
- 修改前先检查现有实现与类型定义，优先复用已有工具（如 `tokenStorage`、`http`、`Result`、`PageResult`、`AgentFactory`）。

## 常用命令

### 前端（`frontend/`）

```bash
npm install
npm run dev        # Vite dev server，默认 http://localhost:5173
npm run build      # tsc --noEmit && vite build
npm run lint       # ESLint flat config
npm run format     # Prettier 格式化 src
npm run preview    # 预览 dist
```

### 后端（`backend/`）

要求 `JAVA_HOME` 指向 JDK 21。

```bash
docker compose up -d                                   # 在仓库根目录启动 MySQL/Redis/MinIO
mvn spring-boot:run                                    # 默认 MySQL + Redis
mvn spring-boot:run "-Dspring-boot.run.profiles=dev"   # 无 Docker：H2 + 内存 TokenStore
mvn -DskipTests package
mvn test
```

当前有 20 个后端单元测试。运行单个测试使用 `mvn -Dtest=类名 test`；前端暂未配置自动化测试框架。

### 本地地址

- 前端：`http://localhost:5173`
- 后端：`http://localhost:8080`
- Swagger：`http://localhost:8080/swagger-ui.html`
- WebSocket：`ws://localhost:8080/ws/chat?token=<accessToken>`

## 配置与 Profile

- 后端通过 `spring.config.import` 自动读取 `backend/.env`（不存在则忽略，系统环境变量优先级更高）；变量清单在 `backend/.env.example` 与 `frontend/.env.example`。前端 `.env` 仍由 Vite 读取。
- `StartupInfoRunner` 在启动完成后打印实际生效的 MySQL（Druid URL、账号、池参数）与 Redis（host/port/db，密码隐藏），并对 Redis 执行 PING；失败只告警，不阻断启动。
- 默认 Profile 连接 MySQL（Druid 连接池）+ Redis，支持通过 `DB_URL` / `DB_HOST` 等环境变量接入外部 MySQL；`dev` Profile 使用 H2（MySQL 兼容模式，同样走 Druid）与 `InMemoryTokenStore`，H2 数据在重启后丢失。
- 生产敏感配置必须来自环境变量，尤其是 `JWT_SECRET`、数据库密码、模型厂商 Key。`application.yml` 中的值仅是本地开发默认值。
- 前端不建 `.env` 时走 Vite 代理：`/api`、`/ws` → `localhost:8080`。`VITE_API_BASE_URL` 默认 `/api/v1`。
- AgentScope 默认关闭（`AGENTSCOPE_ENABLED=false`），使用 `MockModelProvider`，不发起网络请求。启用真实模型需同时设置 `AGENTSCOPE_ENABLED=true` 与 `DEEPSEEK_API_KEY`，默认模型 `deepseek:deepseek-v4-flash`；启用但缺 Key 会启动失败（刻意设计）。
- 视频服务通过 `VIDEO_PROVIDER=mock|seedance` 切换；Seedance 需要 `ARK_API_KEY`，默认视频模型为 `doubao-seedance-2-5-260628`。`SEEDANCE_STATUS_SYNC_MODE` 支持 `polling` / `callback` / `hybrid`。

## 架构要点

### 仓库结构

- `frontend/`：React 18 + TypeScript + Vite 5 + AntD 5 + Zustand + React Router 6。
- `backend/`：Java 21 + Spring Boot 3.3.5，单模块 Maven，MyBatis-Plus 3.5.9 + Druid + MySQL/H2。
- `docs/architecture.md` 是模块与流程的详细说明；`docs/api-convention.md` 是接口契约；`docs/dev-guide.md` 是环境与规范。
- 根目录不放前后端依赖文件。

### 后端

- 统一响应 `Result<T>`（`code/message/data`，成功 `code=0`），分页 `Result<PageResult<T>>`；业务错误抛 `BizException`，由 `GlobalExceptionHandler` 转换。HTTP 状态与业务 code 对应，code 定义见 `common/ResultCode.java`。
- 认证：access + refresh 双 JWT；refresh 刷新时旋转并消费旧 `jti`；`TokenStore` 有 Redis 白名单/黑名单与内存两种实现。JWT 过滤器写入 `UserPrincipal`，业务层用 `SecurityUtils.currentUserId()`。
- 公开路径仅注册、登录、刷新、`/ws/**`、Swagger、`/error`；其余接口必须登录。
- 对话链路：用户发送提示词后，Agent 先调用 `proposeVideoPrompt` 生成原版/优化版提示词和结构化分析步骤，保存为 `PROMPT_PROPOSAL`，此时不创建视频任务。用户通过 `POST /chat/messages/{id}/prompt-decision` 选择版本后，后端才调用 `generateVideoClip` 并创建 `VIDEO_CARD`。
- 工具确认：`AgentScopeChatAgent` 消费 `streamEvents` 事件流，权限上下文为 `BYPASS + addAskRule("generateVideoClip", ASK)`（默认 `DEFAULT` 模式会绕过权限引擎，不触发 `RequireUserConfirmEvent`）；恢复时用 `Msg.METADATA_CONFIRM_RESULTS` + 原 `ToolUseBlock` 再次 `streamEvents`。`POST /chat/messages/{id}/confirm` 由 `PendingConfirmationRegistry` 原子 claim 防重复，挂起记录是单实例内存（TTL 由 `AGENTSCOPE_CONFIRM_TIMEOUT` 控制），卡片 UI 状态存 `chat_messages.metadata`。
- Agent 选择由配置驱动：`MockModelProvider`（默认）或 `AgentScopeChatAgent` + `ReActAgent`；真实模型通过 `ModelRegistry.resolve("deepseek:<model>")` 交给 `agentscope-extensions-model-openai` 的 DeepSeek provider；工具在 `AgentScopeConfig` 注册。
- WebSocket 事件为 `{type, data}`：`chat.chunk`、`chat.done`、`chat.tool_confirm_required`、`chat.tool_confirm_result`、`video.progress`、`pong`。握手通过 query token 鉴权。
- 视频任务：`VideoTaskWorker` 使用数据库状态、轮询时间与租约调度；Seedance 成功后立即流式转存 MinIO，通过预签名 URL 播放，并记录 token 与费用快照。
- 主键使用数据库自增（避免 JS 精度问题）；所有表有 `deleted` 逻辑删除字段。
- MyBatis-Plus 3.5.9 的分页依赖独立的 `mybatis-plus-jsqlparser`，不要误删。

### 前端

- `src/utils/tokenStorage.ts` 是 token 的唯一读取/写入源；Axios 拦截器、WebSocket、路由守卫都从它取值。
- 401 由 `src/api/request.ts` 单飞刷新并重放请求；刷新失败清 token 并跳转 `/create?auth=expired` 重新弹登录。不要在各页面重复实现刷新逻辑。
- `chatStore` 发送消息时先插入乐观用户消息与 streaming 占位，再用 `requestId` 关联 WebSocket 分片与 POST 结果；WS 已连接时 5 秒未收到 `chat.done` 则用同步结果兜底。
- `useChatSocket(enabled)` 负责 25 秒心跳、指数退避重连及 `chat.chunk` / `chat.done` / `video.progress` 分发；游客不建连。
- `PromptProposalCard` 展示提示词优化步骤、原版/优化版和“确认并生成”操作；`GenerationCard` 关联持久化视频任务并每秒更新等待时长。
- 游客草稿存于 `sessionStorage`（`draftStore`，24h TTL）；`authGate` 保存挂起动作，登录成功后 `completeGate` 继续生成或进入作品页。
- `RootLayout` 在 Router 内渲染全局 `AuthModal`；`Brand` 使用 `<Link>`，任何渲染它的组件都必须在 Router 上下文中。
- 接口调用统一走 `src/api/<模块>/index.ts`；全局样式集中在 `src/index.css`，使用其中的 CSS 变量。
- 路由：`/`、`/create`、`/login`、`/register` 公开；`/works` 位于 `ProtectedRoute` + `WorksLayout` 下；`/chat` → `/create`、`/project` → `/works`。

## 修改时必须遵守

1. **接口变更同步文档**：新增/修改接口时更新 `docs/api-convention.md`，保持前端 `src/api` 与后端 Controller 一致。
2. **保持接口契约**：路径以 `/api/v1` 开头，RESTful 风格；分页参数 `page`/`size`，`size` 上限 100；响应统一用 `Result<T>`。
3. **数据归属**：所有用户数据查询带 `userId`；跨用户访问返回 NOT_FOUND，不泄露资源存在性。
4. **敏感信息**：数据库密码、JWT 密钥、AI/视频厂商 Key 一律通过环境变量，禁止写入源码、配置默认值之外的硬编码或提交 `.env`。
5. **技术版本约束**：保持 React 18、React Router v6、Vite 5、Spring Boot 3.x、Java 21；升级大版本需先征得用户同意。
6. **外部服务边界**：DeepSeek 与 Seedance 已接入，不要擅自更换模型厂商或视频厂商。Mock 仍用于本地无外部调用测试；真实生成可能产生费用，必须通过用户确认。
7. **提交前验证**：
   - 前端至少执行 `npm run build` 与 `npm run lint`。
   - 后端至少执行 `mvn -DskipTests package`（JDK 21）。
   - 涉及数据库结构时同步更新 `db/schema.sql` 与 `db/schema-h2.sql`。
8. **Git**：仓库已初始化，默认分支 `main`；分支与提交信息规范见 `docs/dev-guide.md`。未经用户要求不要 commit 或 push。

## 已知限制

- WebSocket 的流式输出是完整回复切片，不是模型 token 级流式（Mock 与 DeepSeek 模式相同）。
- 意图识别：真实模式优先从 AgentScope 工具事件提取 `toolName`，缺失时回退关键词；Mock 模式仍只用关键词。
- 模型调用失败时不写入用户/助手消息，聊天请求暂无完整幂等机制。
- 视频任务由单实例数据库调度器推进，高并发时仍需升级 Redis Stream / MQ 和分布式租约。
- 工具确认挂起记录仅存于单实例内存，重启或多实例切换后确认返回 `40900`；同一会话同时只允许一个待确认。
- 确认接口同步执行 Agent 恢复（预算 `AGENTSCOPE_TIMEOUT + 2s`），真实长耗时工具需改为异步任务。
- MinIO 已实现；验证码、限流和高可用任务队列尚未实现。
