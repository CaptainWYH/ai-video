# 开发指南

## 1. 环境准备

| 工具 | 版本 | 说明 |
| --- | --- | --- |
| JDK | 21 | 后端编译与运行 |
| Maven | 3.9+ | 后端构建 |
| Node.js | 20+ | 前端构建 |
| npm | 10+ | 依赖管理 |
| Docker | 可选 | MySQL / Redis / MinIO |

确认版本：

```bash
java -version
mvn -version
node -v
npm -v
```

> Windows 下若默认 `JAVA_HOME` 指向 JDK 17，请切换到 JDK 21 后再执行 Maven，否则 `maven.compiler.release=21` 会失败。

## 2. 配置与启动

### 2.1 后端

敏感配置通过环境变量读取。后端会自动加载 `backend/.env`（`spring.config.import`，文件不存在则忽略；也兼容从仓库根目录启动），**系统环境变量优先级高于 `.env`**。

首次配置：

```powershell
cd backend
Copy-Item .env.example .env
notepad .env
```

之后直接启动即可，`.env` 已由 `.gitignore` 忽略：

```powershell
$env:JAVA_HOME = "C:\Users\<you>\.jdks\ms-21.x"
mvn spring-boot:run
```

启动完成后 `StartupInfoRunner` 会打印实际生效的数据源，例如：

```text
MySQL  [Druid] : url=jdbc:mysql://<host>:3306/ai_video?...
MySQL  [Druid] : username=..., driver=com.mysql.cj.jdbc.Driver
MySQL  [Druid] : initialSize=5, minIdle=5, maxActive=20, maxWait=60000ms
Redis          : host=..., port=6379, database=0, ssl=false, password=******
TokenStore     : redis
Redis          : PING=PONG
```

密码不会打印；Redis PING 失败只输出 WARN，不阻断启动。

Git Bash / macOS / Linux：

```bash
cd backend
export JWT_SECRET="replace-with-a-long-random-secret-at-least-32-bytes"
mvn spring-boot:run
```

默认连接 `localhost:3306/ai_video` 与 `localhost:6379`。启动依赖：

```bash
docker compose up -d
```

首次启动 MySQL 数据卷时会自动执行 `backend/src/main/resources/db/schema.sql`。如果已有数据卷但表不存在：

```powershell
Get-Content .\backend\src\main\resources\db\schema.sql -Raw |
  docker exec -i aivideo-mysql mysql -uroot -proot123456 ai_video
```

### 2.1.1 连接外部 MySQL

默认数据源使用 Druid 连接池，连接信息全部来自环境变量，不需要改源码。外部库推荐直接覆盖 `DB_URL`（可带 SSL、连接超时等参数），只改主机时用 `DB_HOST` / `DB_PORT` / `DB_NAME`：

```powershell
$env:DB_URL = "jdbc:mysql://db.example.com:3306/ai_video?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&useSSL=true&requireSSL=true"
$env:DB_USERNAME = "ai_video"
$env:DB_PASSWORD = "<password>"
# 首次联通外部库时可自动建表（schema.sql 全部使用 CREATE TABLE IF NOT EXISTS）
$env:DB_INIT_MODE = "always"
cd backend
mvn spring-boot:run
```

`DB_INIT_MODE=always` 默认关闭；建表完成后改回 `never`，避免每次启动都执行初始化脚本。以上变量也可以直接写进 `backend/.env`，效果相同。

Druid 池参数通过 `DB_POOL_INITIAL_SIZE` / `DB_POOL_MIN_IDLE` / `DB_POOL_MAX_ACTIVE` / `DB_POOL_MAX_WAIT` 调整，默认 `5 / 5 / 20 / 60000ms`。连接校验使用 `SELECT 1`，慢 SQL 阈值 2000ms。生产环境请按数据库规格设置，不要把生产账号写进配置文件。

> 说明：当前只启用 Druid 的 `stat` 过滤器。`wall` 与 `mergeSql` 依赖 Guava，需要时自行引入 `com.google.guava:guava` 再追加到 `spring.datasource.druid.filters`。

### 2.2 无 Docker 的 dev profile

`application-dev.yml` 使用 H2 内存库（MySQL 兼容模式）和内存 TokenStore，适合快速体验：

```powershell
cd backend
$env:JAVA_HOME = "C:\Users\<you>\.jdks\ms-21.x"
mvn spring-boot:run "-Dspring-boot.run.profiles=dev"
```

特点：

- 数据在进程退出后丢失，每次启动重新执行 `db/schema-h2.sql`。
- 不需要 MySQL、Redis、MinIO。
- 视频任务、对话、认证流程与正式配置一致。
- **不要**在生产使用 `dev` profile；`memory` TokenStore 不支持多实例，也不持久化。

### 2.3 前端

```bash
cd frontend
npm install
npm run dev
```

默认地址 <http://localhost:5173>。Vite 已配置代理：

| 前端路径 | 代理目标 |
| --- | --- |
| `/api` | `http://localhost:8080` |
| `/ws` | `ws://localhost:8080` |

因此不创建 `.env` 也能联调。若希望直连后端或部署到其他域名：

```bash
cp .env.example .env
```

```dotenv
VITE_API_BASE_URL=http://localhost:8080/api/v1
VITE_WS_URL=ws://localhost:8080/ws/chat
```

修改 `.env` 后需重启 Vite。

## 3. 常用命令

### 前端

```bash
npm run dev        # 开发服务器
npm run build      # tsc --noEmit + vite build
npm run preview    # 预览产物
npm run lint       # ESLint
npm run format     # Prettier 格式化 src
```

### 后端

```bash
mvn spring-boot:run                                  # 默认 MySQL + Redis
mvn spring-boot:run "-Dspring-boot.run.profiles=dev" # H2 + 内存 TokenStore
mvn -DskipTests package                              # 打包
mvn test                                             # 运行全部后端单元测试
```

构建成功后产物位于 `backend/target/*.jar`，可通过 `java -jar` 启动。

## 4. 配置说明

### 4.1 Profile

| Profile | 数据库 | TokenStore | 用途 |
| --- | --- | --- | --- |
| 默认 | MySQL | Redis | 接近生产的本地联调 |
| `dev` | H2（内存） | memory | 无依赖快速体验 |

### 4.2 关键环境变量

| 变量 | 作用 | 备注 |
| --- | --- | --- |
| `JWT_SECRET` | JWT 签名密钥 | 生产必须替换为足够长的随机值 |
| `JWT_ACCESS_TTL` | access 有效期 | ISO-8601，如 `PT30M` |
| `JWT_REFRESH_TTL` | refresh 有效期 | ISO-8601，如 `P7D` |
| `TOKEN_STORE` | `redis` / `memory` | 生产使用 `redis` |
| `DB_URL` | 完整 JDBC 串，外部数据库优先使用 | 不填则由 `DB_HOST` 等拼装 |
| `DB_HOST` / `DB_PORT` / `DB_NAME` / `DB_USERNAME` / `DB_PASSWORD` | MySQL 连接 | Druid 连接池 |
| `DB_INIT_MODE` | 是否自动执行 `db/schema.sql` | `never` / `always` |
| `DB_POOL_*` | Druid 初始、最小、最大连接数与等待时间 | 见 `.env.example` |
| `REDIS_*` | Redis 连接 | 见 `.env.example` |
| `AGENTSCOPE_ENABLED` | 是否启用真实 Agent | 默认 `false` |
| `AGENTSCOPE_MODEL` | 模型标识，需 `deepseek:` 前缀 | `deepseek:deepseek-v4-flash` |
| `DEEPSEEK_API_KEY` | DeepSeek Key | 启用真实模型时必填 |
| `DEEPSEEK_BASE_URL` | DeepSeek / 兼容网关地址 | `https://api.deepseek.com` |
| `AGENTSCOPE_TIMEOUT` | 单次模型调用超时 | `PT15S`（需小于前端 20s） |
| `AGENTSCOPE_MAX_ITERS` / `AGENTSCOPE_MAX_ATTEMPTS` | ReAct 迭代上限 / 模型调用尝试次数 | `3` / `1` |
| `AGENTSCOPE_STREAM` | 是否开启模型流式（获取 thinking 增量） | 默认 `true`；关闭后仍展示工具/状态步骤 |
| `AGENTSCOPE_CONFIRM_TIMEOUT` | 工具确认挂起有效期 | ISO-8601，默认 `PT5M` |
| `VIDEO_PROVIDER` | 视频服务：`mock` / `seedance` | 默认 `mock` |
| `ARK_API_KEY` | 火山方舟 API Key | Seedance 模式必填 |
| `SEEDANCE_MODEL` | Seedance Model ID | `doubao-seedance-2-5-260628` |
| `SEEDANCE_STATUS_SYNC_MODE` | `polling` / `callback` / `hybrid` | 本地推荐 `polling` |
| `SEEDANCE_CALLBACK_BASE_URL` | 公网 HTTPS 服务地址 | callback / hybrid 可选 |
| `SEEDANCE_CALLBACK_SECRET` | 回调路径 HMAC 密钥 | 配合回调地址使用 |
| `SEEDANCE_POLL_INTERVAL` | 轮询间隔 | 默认 `PT5S` |

完整清单见 [`backend/.env.example`](../backend/.env.example)。不要提交真实 `.env` 或密钥；`.gitignore` 已忽略 `.env`。

### 4.3 启用 DeepSeek 真实模型

1. 设置 `AGENTSCOPE_ENABLED=true`。
2. 设置 `DEEPSEEK_API_KEY`（必填）；如走代理/私有网关，再设置 `DEEPSEEK_BASE_URL`。
3. 按需调整 `AGENTSCOPE_MODEL`（默认 `deepseek:deepseek-v4-flash`）与 `AGENTSCOPE_TIMEOUT`。
4. 重启后端，确认日志出现 `AgentScope 真实模型: model=deepseek:...`。

未启用时使用 `MockModelProvider`，不会发起任何网络请求。注意：

- `AGENTSCOPE_ENABLED=true` 但 `DEEPSEEK_API_KEY` 为空时会**启动失败**，这是刻意设计，避免静默回退掩盖配置错误。
- 模型调用的超时/限流/鉴权失败/5xx 分别映射为业务码 `50400` / `42900` / `50200` / `50300`（见 `docs/api-convention.md`）。鉴权失败返回 502 而非 401，不会触发前端重新登录。
- 模型调用发生在数据库事务之外；调用失败时不写入用户/助手消息。

### 4.3.1 工具调用确认演示

真实模式默认对 `generateVideoClip` 开启「调用前用户确认」：

1. 用户在 `/create` 输入视频提示词。
2. Agent 调用 `proposeVideoPrompt`，返回原版提示词、优化版提示词和 3-5 条可展示的规划步骤；此时不会创建视频任务。
3. 对话中的 `PROMPT_PROPOSAL` 卡片展示分析步骤、工具调用和两版提示词。
4. 用户选择「使用优化版并生成」或「使用原版并生成」。
5. 后端按选定提示词调用 `generateVideoClip`，创建并关联视频任务；对话卡片开始按秒计时。
6. 已选择的方案不能重复提交。`generateVideoClip` 仍保留 AgentScope 权限规则，兼容其他直接工具调用入口。

`generateVideoClip` 工具参数：

| 参数 | 必填 | 说明 |
| --- | --- | --- |
| `prompt` | 是 | 视频画面提示词 |
| `style` | 否 | `cinematic` / `realistic` / `anime` / `cyberpunk` / `ink` |
| `aspect` | 否 | `16:9` / `9:16` / `1:1` / `4:3` / `3:4` / `21:9` / `adaptive` |
| `durationSeconds` | 否 | 4～30 秒 |
| `resolution` | 否 | `480p` / `720p` / `1080p` |
| `generateAudio` | 否 | 是否生成同步音频 |
| `watermark` | 否 | 是否添加水印 |
| `outputFormat` | 否 | `mp4` / `mov` |

界面或 API 已提供的 `VideoGenerationParams` 优先；工具参数只补齐缺失字段。

确认参数只提交 `approved`，服务端以挂起记录中的原始 `toolCalls` 为准。挂起状态保存在单实例内存，多实例部署需改用共享存储。

真实模式下仍待完善：

- 使用 AgentScope 的会话/记忆机制替代手动拼接历史。
- token 级真流式（当前仍是完整回复切片推送）。
- 自动重试/退避/熔断（当前 `AGENTSCOPE_MAX_ATTEMPTS` 默认 1）。
- 工具确认为单机内存实现，重启/多实例失效。

### 4.4 MinIO

`storage` 包已实现 MinIO 流式上传和预签名 URL。Seedance 成功后会在官方 24 小时链接过期前转存：

1. 保持 MinIO endpoint 对浏览器可访问，或通过网关暴露。
2. 不要把 AccessKey / SecretKey 写入代码或提交到仓库，继续使用环境变量。
3. 预签名有效期由 `MINIO_PRESIGNED_TTL` 控制，默认 15 分钟。
4. 已有数据库升级前执行 `backend/src/main/resources/db/seedance-upgrade.sql`。

## 5. 代码约定

### 5.1 后端

- 包结构：`controller / service / service.impl / dto / entity / mapper`。
- Controller 只接收参数、调用 Service、返回 `Result<T>`，不写业务逻辑。
- 查询当前用户数据必须带 `userId` 条件；跨用户访问统一返回 `40400`，避免泄露资源存在性。
- 业务错误使用 `BizException`，由 `GlobalExceptionHandler` 转换。
- 公共方法、复杂业务分支写 Javadoc 或行内注释，说明「为什么」而非重复代码。
- 对外接口添加 `@Operation`、`@Tag`、`@Schema` 注解，并登记到 `docs/api-convention.md`。
- 敏感信息（密码、token、密钥）不得写入日志。
- 时间字段使用 `LocalDateTime`，Jackson 统一 `Asia/Shanghai`。

### 5.2 前端

- 使用 `@/` 路径别名，禁止深层相对路径（如 `../../../`）。
- 全局状态放 Zustand store；页面局部状态用 `useState`。
- token 只通过 `tokenStorage` 读写；组件不要直接操作 `localStorage` 的 token 键。
- 接口调用统一走 `src/api/*`，不要在组件中直接使用 axios。
- 类型定义放 `src/types`，接口返回值必须显式标注类型。
- 样式使用 `src/index.css` 中的 CSS 变量与现有类名；新增颜色优先引用变量。
- 提交前必须通过 `npm run build` 与 `npm run lint`。

## 6. Git 规范

仓库采用单一 Git 仓库，前后端同仓管理。

### 6.1 分支

| 分支 | 用途 |
| --- | --- |
| `main` | 可发布分支，只接受合并 |
| `dev` | 集成分支 |
| `feature/<scope>-<short-desc>` | 功能分支，例如 `feature/chat-streaming` |
| `fix/<scope>-<short-desc>` | 修复分支，例如 `fix/auth-refresh-loop` |
| `docs/<short-desc>` | 文档分支 |

禁止直接向 `main` 推送；功能开发从 `dev` 切出，完成后发起合并请求。

### 6.2 提交信息

采用 Conventional Commits：

```text
<type>(<scope>): <subject>

[optional body]
```

常用 type：

| type | 含义 |
| --- | --- |
| `feat` | 新功能 |
| `fix` | 缺陷修复 |
| `refactor` | 重构（不改变行为） |
| `docs` | 文档 |
| `style` | 格式调整 |
| `test` | 测试 |
| `chore` | 构建、依赖、配置 |

示例：

```text
feat(chat): 支持 WebSocket 模拟流式回复
fix(auth): 修复 refresh token 旋转后并发 401
docs(api): 补充视频任务接口字段说明
```

提交前检查：

- [ ] 不包含 `.env`、密钥、真实 token、个人路径。
- [ ] 前端 `npm run build`、`npm run lint` 通过。
- [ ] 后端 `mvn -DskipTests package` 通过。
- [ ] 新增接口已登记到 `docs/api-convention.md`。
- [ ] 数据库变更同步更新 `schema.sql` 与 `schema-h2.sql`。

## 7. 常见问题

### Q1. Maven 报 `release version 21 not supported`

`JAVA_HOME` 仍指向 JDK 17。切换到 JDK 21 并确认 `mvn -version` 中的 Java 版本。

### Q2. 后端启动报数据库连接失败

- 确认 `docker compose ps` 中 MySQL 状态为 healthy。
- 确认 `DB_*` 环境变量与容器配置一致。
- 无 Docker 时改用 `dev` profile。

### Q3. 用户名或邮箱已存在

注册接口返回 `40900`。换一个用户名/邮箱，或清理数据库后重试。

### Q4. 前端请求 401 并跳回登录页

- access token 过期且 refresh 失败（refresh 过期、已被轮换、Redis 数据丢失）。
- `dev` profile 重启后内存 TokenStore 清空，旧 token 全部失效，需要重新登录。
- 检查浏览器 localStorage 中的 `aivideo.auth.accessToken` / `aivideo.auth.refreshToken`。

### Q5. WebSocket 一直显示「离线兜底」

- 检查后端 `/ws/chat` 是否可访问、token 是否有效。
- 若使用 `VITE_WS_URL` 直连，确认地址、端口和协议（`ws://` / `wss://`）。
- 跨域部署时需配置反向代理转发 Upgrade 头。
- 离线时前端仍会通过 POST 同步结果展示回复，不影响基本功能。

### Q6. 端口被占用

```powershell
Get-NetTCPConnection -LocalPort 8080 | Select-Object OwningProcess
Stop-Process -Id <pid>
```

或通过 `SERVER_PORT` 修改后端端口，并同步修改前端代理 / `VITE_API_BASE_URL`。

### Q7. 视频任务一直 PENDING / RUNNING

视频任务由数据库状态和定时调度器持续推进，应用重启后会恢复轮询/转存；高并发场景可再升级为 Redis Stream / MQ。

## 8. 本地验收清单

```text
[ ] docker compose up -d（或使用 dev profile）
[ ] 后端启动成功，/swagger-ui.html 可访问
[ ] 前端 npm run dev 启动成功
[ ] 首次访问门户 → 点击「立即开始」进入 /create，游客可直接输入提示词
[ ] 游客点击「生成视频」→ 同页弹登录；关闭弹窗后草稿仍在，刷新后草稿仍可恢复
[ ] 发送视频提示词后先出现 `PROMPT_PROPOSAL`，此时作品页没有新增视频任务
[ ] 方案卡展示分析步骤、`proposeVideoPrompt` 和原版/优化版提示词
[ ] 选择原版或优化版后才出现 `VIDEO_CARD`，参数与界面选择一致
[ ] 视频卡片按秒计时，Seedance 状态通过 WebSocket 更新
[ ] 已登录访问 / → 直接进入 /create；未登录访问 /works → 回到 /create 并弹出「登录后查看作品」
[ ] 作品页展示生成记录，筛选、刷新、重试可用
[ ] 刷新页面后历史消息仍存在
[ ] 退出登录 → 回到门户
[ ] npm run build / npm run lint 通过
[ ] mvn test 通过
[ ] mvn -DskipTests package 通过
```
