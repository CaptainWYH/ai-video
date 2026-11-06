-- ============================================================
-- AI Video Studio - H2 (MODE=MySQL) schema
-- 仅用于 dev profile 的无依赖本地烟测
-- ============================================================

CREATE TABLE IF NOT EXISTS users (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    username    VARCHAR(50)  NOT NULL UNIQUE,
    email       VARCHAR(100) NOT NULL UNIQUE,
    password    VARCHAR(100) NOT NULL,
    nickname    VARCHAR(50)  DEFAULT NULL,
    avatar_url  VARCHAR(500) DEFAULT NULL,
    status      TINYINT      NOT NULL DEFAULT 1,
    created_at  DATETIME     NOT NULL,
    updated_at  DATETIME     NOT NULL,
    deleted     TINYINT      NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS chat_sessions (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id         BIGINT       NOT NULL,
    title           VARCHAR(200) NOT NULL DEFAULT '新会话',
    last_message_at DATETIME     DEFAULT NULL,
    created_at      DATETIME     NOT NULL,
    updated_at      DATETIME     NOT NULL,
    deleted         TINYINT      NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS chat_messages (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    session_id   BIGINT      NOT NULL,
    user_id      BIGINT      NOT NULL,
    role         VARCHAR(20) NOT NULL,
    content      TEXT        NOT NULL,
    message_type VARCHAR(30) NOT NULL DEFAULT 'TEXT',
    metadata     TEXT        DEFAULT NULL,
    created_at   DATETIME    NOT NULL,
    updated_at   DATETIME    NOT NULL,
    deleted      TINYINT     NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS projects (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id     BIGINT        NOT NULL,
    name        VARCHAR(200)  NOT NULL,
    description VARCHAR(1000) DEFAULT NULL,
    script      TEXT          DEFAULT NULL,
    storyboard  TEXT          DEFAULT NULL,
    created_at  DATETIME      NOT NULL,
    updated_at  DATETIME      NOT NULL,
    deleted     TINYINT       NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS video_tasks (
    id                          BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id                     BIGINT        NOT NULL,
    project_id                  BIGINT        DEFAULT NULL,
    chat_session_id             BIGINT        DEFAULT NULL,
    chat_message_id             BIGINT        DEFAULT NULL,
    prompt                      TEXT          NOT NULL,
    params                      TEXT          DEFAULT NULL,
    status                      VARCHAR(20)   NOT NULL DEFAULT 'PENDING',
    progress                    INT           NOT NULL DEFAULT 0,
    result_url                  VARCHAR(1000) DEFAULT NULL,
    error_message               VARCHAR(1000) DEFAULT NULL,
    provider                    VARCHAR(32)   NOT NULL DEFAULT 'MOCK',
    provider_task_id            VARCHAR(128)  DEFAULT NULL,
    provider_status             VARCHAR(32)   DEFAULT NULL,
    provider_model              VARCHAR(128)  DEFAULT NULL,
    provider_error_code         VARCHAR(100)  DEFAULT NULL,
    provider_result_url         VARCHAR(1500) DEFAULT NULL,
    storage_key                 VARCHAR(800)  DEFAULT NULL,
    client_request_id           VARCHAR(100)  DEFAULT NULL,
    completion_tokens           BIGINT        DEFAULT NULL,
    total_tokens                BIGINT        DEFAULT NULL,
    unit_price_cny_per_million  DECIMAL(12,4) DEFAULT NULL,
    pricing_version             VARCHAR(50)   DEFAULT NULL,
    estimated_cost_cny          DECIMAL(18,6) DEFAULT NULL,
    currency                    VARCHAR(8)    DEFAULT 'CNY',
    submitted_at                DATETIME      DEFAULT NULL,
    last_polled_at              DATETIME      DEFAULT NULL,
    next_poll_at                DATETIME      DEFAULT NULL,
    completed_at                DATETIME      DEFAULT NULL,
    lease_until                 DATETIME      DEFAULT NULL,
    attempt_count               INT           NOT NULL DEFAULT 0,
    version                     INT           NOT NULL DEFAULT 0,
    created_at                  DATETIME      NOT NULL,
    updated_at                  DATETIME      NOT NULL,
    deleted                     TINYINT       NOT NULL DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_chat_sessions_user_id ON chat_sessions (user_id);
CREATE INDEX IF NOT EXISTS idx_chat_sessions_last_message_at ON chat_sessions (last_message_at);
CREATE INDEX IF NOT EXISTS idx_chat_messages_session_id ON chat_messages (session_id);
CREATE INDEX IF NOT EXISTS idx_chat_messages_user_id ON chat_messages (user_id);
CREATE INDEX IF NOT EXISTS idx_projects_user_id ON projects (user_id);
CREATE INDEX IF NOT EXISTS idx_video_tasks_user_id ON video_tasks (user_id);
CREATE INDEX IF NOT EXISTS idx_video_tasks_project_id ON video_tasks (project_id);
CREATE UNIQUE INDEX IF NOT EXISTS uk_video_tasks_user_request
    ON video_tasks (user_id, client_request_id);
CREATE UNIQUE INDEX IF NOT EXISTS uk_video_tasks_provider_task
    ON video_tasks (provider, provider_task_id);
CREATE INDEX IF NOT EXISTS idx_video_tasks_chat_message_id ON video_tasks (chat_message_id);
CREATE INDEX IF NOT EXISTS idx_video_tasks_due ON video_tasks (status, next_poll_at);
