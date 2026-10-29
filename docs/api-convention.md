# API 规范

## 1. 基本约定

| 项目 | 约定 |
| --- | --- |
| 基础路径 | `/api/v1` |
| 风格 | RESTful，资源使用复数名词、小写中划线 |
| 传输 | JSON，`Content-Type: application/json` |
| 鉴权 | `Authorization: Bearer <accessToken>` |
| 时间 | ISO-8601 本地时间，例如 `2026-10-27T14:30:00` |
| ID | 数据库自增 `Long`，前端按 `number` 处理 |
| 接口文档 | Swagger UI `/swagger-ui.html`，OpenAPI `/v3/api-docs` |

公开接口（无需 token）：`POST /auth/register`、`POST /auth/login`、`POST /auth/refresh`、`WS /ws/chat` 握手、Swagger、`/error`。其余接口均需登录。

## 2. 统一响应

### 2.1 成功

```json
{
  "code": 0,
  "message": "success",
  "data": {}
}
```

`data` 可为对象、数组、分页结构或 `null`（无返回值的操作）。

### 2.2 失败

```json
{
  "code": 40100,
  "message": "用户名或密码错误",
  "data": null
}
```

HTTP 状态码与业务错误码对应；前端同时检查 HTTP 状态与 `code`。

### 2.3 错误码

| code | HTTP | 含义 |
| --- | --- | --- |
| `0` | 200 | 成功 |
| `40000` | 400 | 请求参数错误 / 校验失败 |
| `40100` | 401 | 未登录、token 过期、账号或密码错误 |
| `40300` | 403 | 无权限 |
| `40400` | 404 | 资源不存在（含跨用户访问） |
| `40900` | 409 | 资源冲突（如用户名、邮箱已存在） |
| `50000` | 500 | 服务器内部错误 |

参数校验失败时 `message` 形如 `参数校验失败: username, password`；详细字段错误写入后端日志。

### 2.4 分页

请求参数统一为 `page`（从 1 开始）与 `size`；响应统一为：

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "page": 1,
    "size": 20,
    "total": 42,
    "records": []
  }
}
```

后端会将 `size` 限制在 `1-100`。会话消息按 `id` 升序返回。

## 3. 接口清单

### 3.1 认证 `/api/v1/auth`

| 方法 | 路径 | 鉴权 | 说明 |
| --- | --- | --- | --- |
| POST | `/register` | 否 | 注册并自动登录 |
| POST | `/login` | 否 | 登录 |
| POST | `/refresh` | 否 | 刷新并轮换 token |
| POST | `/logout` | 是（可选） | 登出并作废 token |
| GET | `/me` | 是 | 当前用户信息 |

#### POST `/register`

```json
{
  "username": "demo_user",
  "email": "demo@example.com",
  "password": "123456",
  "nickname": "演示用户"
}
```

校验：用户名 3-50 位，仅字母/数字/下划线；邮箱合法且不超过 100 字符；密码 6-64 位；昵称不超过 50 字符。

返回 `TokenResponse`：

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "accessToken": "eyJhbGciOi...",
    "refreshToken": "eyJhbGciOi...",
    "tokenType": "Bearer",
    "expiresIn": 1800,
    "user": {
      "id": 1,
      "username": "demo_user",
      "email": "demo@example.com",
      "nickname": "演示用户",
      "avatarUrl": null,
      "createdAt": "2026-10-27T14:30:00"
    }
  }
}
```

#### POST `/login`

```json
{ "account": "demo_user", "password": "123456" }
```

`account` 支持用户名或邮箱，返回结构同注册。账号或密码错误返回 `40100`。

#### POST `/refresh`

```json
{ "refreshToken": "eyJhbGciOi..." }
```

成功返回新的 access + refresh token（旧 refresh 立即失效）。refresh 无效或已使用返回 `40100`。

#### POST `/logout`

请求头可带 access token，请求体可选：

```json
{ "refreshToken": "eyJhbGciOi..." }
```

当前 access token 的 `jti` 加入黑名单，refresh token 从白名单删除。返回：

```json
{ "code": 0, "message": "success", "data": null }
```

#### GET `/me`

返回 `UserInfoResponse`（字段同 `TokenResponse.user`）。

### 3.2 对话 `/api/v1/chat`

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| POST | `/sessions` | 创建会话 |
| GET | `/sessions?page=1&size=20` | 会话分页列表 |
| GET | `/sessions/{id}/messages?page=1&size=100` | 会话历史消息分页 |
| POST | `/messages` | 发送消息（同步返回 + WS 推流） |

#### POST `/sessions`

请求体可省略：

```json
{ "title": "海边日落分镜" }
```

未传或为空时标题为「新会话」。返回 `ChatSessionResponse`：

```json
{
  "id": 12,
  "title": "新会话",
  "lastMessageAt": null,
  "createdAt": "2026-10-27T14:30:00"
}
```

#### GET `/sessions`

按 `lastMessageAt` 倒序、`id` 倒序。返回 `PageResult<ChatSessionResponse>`。

#### GET `/sessions/{id}/messages`

仅能访问当前用户的会话，否则返回 `40400`。返回 `PageResult<ChatMessageResponse>`，按 `id` 升序。

#### POST `/messages`

```json
{
  "sessionId": 12,
  "content": "帮我生成一个海边日落主题的分镜脚本",
  "requestId": "c1f5d3a8-..."
}
```

| 字段 | 必填 | 说明 |
| --- | --- | --- |
| `sessionId` | 是 | 当前用户的会话 ID |
| `content` | 是 | 1-4000 字符 |
| `requestId` | 否 | 前端生成，最长 64 字符，用于关联 WS 分片 |

同步返回助手消息：

```json
{
  "id": 102,
  "sessionId": 12,
  "role": "ASSISTANT",
  "content": "（Mock 模式）……",
  "messageType": "VIDEO_CARD",
  "videoIntent": true,
  "toolName": "generateStoryboard",
  "createdAt": "2026-10-27T14:31:00"
}
```

`messageType` 取值 `TEXT` / `VIDEO_CARD`；`videoIntent=true` 时前端展示视频占位卡片。首条消息会把会话标题更新为内容前 30 个字符。

### 3.3 视频任务 `/api/v1/video`

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| POST | `/tasks` | 创建视频生成任务（占位） |
| GET | `/tasks/{id}` | 查询任务状态 |
| GET | `/tasks?page=1&size=20` | 任务分页列表 |

#### POST `/tasks`

```json
{
  "prompt": "夕阳下的海边，海浪拍打礁石，电影感，暖色调",
  "params": {
    "style": "cinematic",
    "duration": 5,
    "resolution": "1080p"
  },
  "projectId": null
}
```

| 字段 | 必填 | 说明 |
| --- | --- | --- |
| `prompt` | 是 | 1-2000 字符 |
| `params` | 否 | 原样透传并持久化为 JSON，常用 `style` / `duration` / `resolution` |
| `projectId` | 否 | 关联项目，必须属于当前用户 |

返回 `VideoTaskResponse`：

```json
{
  "id": 7,
  "projectId": null,
  "prompt": "夕阳下的海边……",
  "params": { "style": "cinematic", "duration": 5, "resolution": "1080p" },
  "status": "PENDING",
  "progress": 0,
  "resultUrl": null,
  "errorMessage": null,
  "createdAt": "2026-10-27T14:32:00",
  "updatedAt": "2026-10-27T14:32:00"
}
```

`status`：`PENDING` / `RUNNING` / `SUCCESS` / `FAILED`。占位实现会在数秒内推进到 `SUCCESS`，`resultUrl` 为 `https://example.com/mock-video/{id}.mp4`。

#### GET `/tasks/{id}`

仅能访问当前用户的任务，否则返回 `40400`。

#### GET `/tasks`

按 `id` 倒序返回 `PageResult<VideoTaskResponse>`。

### 3.4 项目 `/api/v1/projects`

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| POST | `/` | 创建项目 |
| GET | `/?page=1&size=20` | 项目分页列表 |
| GET | `/{id}` | 项目详情 |
| PUT | `/{id}` | 全量更新项目 |
| DELETE | `/{id}` | 逻辑删除项目 |

请求体（创建 / 更新相同）：

```json
{
  "name": "海边日落短片",
  "description": "15 秒电影感短片",
  "script": "场景一……",
  "storyboard": "{\"shots\":[]}"
}
```

| 字段 | 必填 | 说明 |
| --- | --- | --- |
| `name` | 是 | 1-200 字符 |
| `description` | 否 | 不超过 1000 字符 |
| `script` | 否 | 剧本文本 |
| `storyboard` | 否 | 分镜 JSON 字符串 |

返回 `ProjectResponse`：`id`、`name`、`description`、`script`、`storyboard`、`createdAt`、`updatedAt`。

## 4. WebSocket 协议

- 地址：`ws://localhost:8080/ws/chat?token=<accessToken>`
- 握手 token 必须是有效 access token 且未被登出。
- 客户端可发送文本 `ping`，服务端返回 `{"type":"pong","data":null}`。
- 所有服务端事件：

```json
{ "type": "<event>", "data": { } }
```

| type | 时机 | data |
| --- | --- | --- |
| `chat.chunk` | 助手回复分片 | `sessionId`、`requestId`、`delta` |
| `chat.done` | 回复完成 | `sessionId`、`requestId`、`message` |
| `video.progress` | 视频任务状态更新 | `taskId`、`status`、`progress`、`resultUrl` |
| `pong` | 心跳响应 | `null` |

说明：

- 当前 `chat.chunk` 是完整回复的模拟切片（每片 6 个字符，间隔 40ms），不是模型 token 级流式。
- 若客户端离线，后端直接跳过推送；REST 接口仍会返回完整消息。
- 前端在收到 `chat.done` 后结束占位气泡；若 5 秒内未收到，则以 POST 返回结果兜底。

## 5. 接口登记流程

新增或修改接口时必须同步更新本文档，并遵循：

1. 路径以 `/api/v1` 开头，资源名使用复数。
2. 使用 `Result<T>` 包装响应，分页返回 `Result<PageResult<T>>`。
3. 请求 DTO 加 Jakarta Validation 注解，Controller 参数加 `@Valid`。
4. Controller 与 DTO 添加 SpringDoc `@Operation` / `@Tag` / `@Schema` 注解。
5. 业务错误抛 `BizException`，不要直接返回裸 `ResponseEntity`。
6. 涉及用户数据的查询必须校验 `userId` 归属。
7. 更新本文档的接口清单、字段说明与示例。
8. 敏感字段（密码、token 明文、密钥）不得出现在日志与响应中。

## 6. 调用示例

```bash
# 注册
curl -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username":"demo_user","email":"demo@example.com","password":"123456"}'

# 创建会话
curl -X POST http://localhost:8080/api/v1/chat/sessions \
  -H "Authorization: Bearer <accessToken>" \
  -H "Content-Type: application/json" \
  -d '{"title":"海边日落分镜"}'

# 发送消息
curl -X POST http://localhost:8080/api/v1/chat/messages \
  -H "Authorization: Bearer <accessToken>" \
  -H "Content-Type: application/json" \
  -d '{"sessionId":1,"content":"帮我生成海边日落的分镜","requestId":"demo-request-1"}'

# 创建视频任务
curl -X POST http://localhost:8080/api/v1/video/tasks \
  -H "Authorization: Bearer <accessToken>" \
  -H "Content-Type: application/json" \
  -d '{"prompt":"夕阳下的海边，电影感","params":{"duration":5,"resolution":"1080p"}}'
```
