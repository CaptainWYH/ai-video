# 仓库贡献指南

## 项目结构与模块组织

本仓库包含 React/TypeScript 前端和 Spring Boot 后端。后端业务模块位于 `backend/src/main/java/com/aivideo/`，按 `controller`、`service`、`dto`、`entity` 和 `mapper` 分层。配置与 SQL 位于 `backend/src/main/resources/`，测试位于 `backend/src/test/java/`。

前端源码位于 `frontend/src/`，按 `api/`、`components/`、`pages/`、`stores/` 和 `types/` 组织。架构、接口契约和开发说明位于 `docs/`；`docker-compose.yml` 用于启动本地 MySQL、Redis 和 MinIO。

## 构建、测试与开发命令

环境要求：JDK 21、Maven 3.9+、Node.js 20+ 和 npm 10+。后端命令在 `backend/` 执行，前端命令在 `frontend/` 执行。

```bash
docker compose up -d                              # 在仓库根目录启动依赖
mvn spring-boot:run                               # 使用 MySQL + Redis
mvn spring-boot:run "-Dspring-boot.run.profiles=dev"  # 使用 H2 + 内存存储
mvn test
mvn -DskipTests package
npm install && npm run dev
npm run build                                     # 类型检查并构建 Vite
npm run lint
npm run format
```

`dev` Profile 无需 Docker，但重启后数据会丢失。

## 编码风格与命名约定

遵循 `.editorconfig`：UTF-8 编码、LF 换行、每行最多 120 个字符；Java 使用 4 空格缩进，其他文件使用 2 空格。Java 类使用 PascalCase，方法和字段使用 camelCase；Controller 保持精简，业务逻辑放入 Service。React 组件和页面目录使用 PascalCase，Hook 以 `use` 开头。

前端使用 Prettier 和 ESLint。优先使用 `@/` 路径别名；HTTP 请求统一通过 `src/api/`，Token 只能通过 `tokenStorage` 读写。保持现有契约：接口以 `/api/v1` 开头，响应使用 `Result<T>` 和 `PageResult<T>`，用户数据查询必须限定 `userId`。

## 测试指南

后端使用 JUnit 5，当前已有 20 个测试。测试类命名为 `<类名>Test.java`，包结构与生产代码一致；运行全部测试使用 `mvn test`，运行单个测试使用 `mvn -Dtest=ModelErrorTranslatorTest test`。前端暂无测试框架，至少应通过 `npm run build && npm run lint`。当前未设置覆盖率门槛。

## 提交与合并请求

提交信息采用 Conventional Commits，例如 `feat(chat): 增加流式输出兜底` 或 `docs: 更新接口示例`。分支使用 `feature/<范围>-<描述>`、`fix/<范围>-<描述>` 或 `docs/<描述>`。

合并请求应说明变更内容、关联 Issue、验证命令；涉及 UI 的变更需提供截图。接口变更需更新 `docs/api-convention.md`，数据库结构变更需同步更新 `db/schema.sql` 和 `db/schema-h2.sql`。

## 配置与安全

本地配置请参考 `backend/.env.example` 和 `frontend/.env.example`。禁止提交 `.env` 文件、凭据、JWT 密钥或模型服务密钥。AgentScope 默认使用 Mock；视频服务默认 `VIDEO_PROVIDER=mock`，配置 `ARK_API_KEY` 后可切换 Seedance。真实视频结果会转存 MinIO。

## 沟通与协作

仓库内的协作和回复统一使用中文；代码、命令、路径和必要技术名词保持原样。
