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
mvn test                                             # 测试（当前暂无用例）
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
| `AGENTSCOPE_MODEL` | 模型标识 | 如 `dashscope:qwen-plus` |
| `DASHSCOPE_API_KEY` | 模型厂商 Key | 启用真实模型时必填 |

完整清单见 [`backend/.env.example`](../backend/.env.example)。不要提交真实 `.env` 或密钥；`.gitignore` 已忽略 `.env`。

### 4.3 启用 AgentScope 真实模型

1. 设置 `AGENTSCOPE_ENABLED=true`。
2. 设置 `AGENTSCOPE_MODEL` 为 AgentScope 支持的模型标识。
3. 注入对应厂商的 API Key（如 `DASHSCOPE_API_KEY`）。
4. 重启后端，确认 `ReActAgent` Bean 创建成功。

未启用时使用 `MockModelProvider`，不会发起任何网络请求。真实模式下仍需完善：

- 从 AgentScope 工具调用事件提取 `toolName`（当前为关键词识别）。
- 使用 AgentScope 的会话/记忆机制替代手动拼接历史。
- 处理模型超时、限流与失败重试。

### 4.4 MinIO

`storage` 包已定义 `ObjectStorageService` 接口与 MinIO 占位实现，但尚未写文件。接入时：

1. 在 `MinioConfig` 中创建 `MinioClient`。
2. 实现 bucket 初始化、上传、预签名 URL。
3. 将视频结果写入对象存储后，把 `resultUrl` 替换为预签名地址。
4. 不要把 AccessKey / SecretKey 写入代码或配置文件，继续走环境变量。

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

占位任务在内存线程池中推进，应用重启会中断。重新创建任务即可；生产接入时应改为持久化队列。

## 8. 本地验收清单

```text
[ ] docker compose up -d（或使用 dev profile）
[ ] 后端启动成功，/swagger-ui.html 可访问
[ ] 前端 npm run dev 启动成功
[ ] 首次访问门户 → 点击「立即开始」进入 /create，游客可直接输入提示词
[ ] 游客点击「生成视频」→ 同页弹登录；关闭弹窗后草稿仍在，刷新后草稿仍可恢复
[ ] 登录成功后自动继续生成 → 出现助手回复、分镜意图与生成卡片，进度推进到 100%
[ ] 已登录访问 / → 直接进入 /create；未登录访问 /works → 回到 /create 并弹出「登录后查看作品」
[ ] 作品页展示生成记录，筛选、刷新、重试可用
[ ] 刷新页面后历史消息仍存在
[ ] 退出登录 → 回到门户
[ ] npm run build / npm run lint 通过
[ ] mvn -DskipTests package 通过
```
