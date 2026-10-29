-- ============================================================
-- AI Video Studio - MySQL 8 schema
-- 首次启动 docker compose 时会自动执行
-- ============================================================

CREATE TABLE IF NOT EXISTS users (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    username    VARCHAR(50)  NOT NULL,
    email       VARCHAR(100) NOT NULL,
    password    VARCHAR(100) NOT NULL,
    nickname    VARCHAR(50)  DEFAULT NULL,
    avatar_url  VARCHAR(500) DEFAULT NULL,
    status      TINYINT      NOT NULL DEFAULT 1,
    created_at  DATETIME     NOT NULL,
    updated_at  DATETIME     NOT NULL,
    deleted     TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_users_username (username),
    UNIQUE KEY uk_users_email (email)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '用户表';

CREATE TABLE IF NOT EXISTS chat_sessions (
    id              BIGINT       NOT NULL AUTO_INCREMENT,
    user_id         BIGINT       NOT NULL,
    title           VARCHAR(200) NOT NULL DEFAULT '新会话',
    last_message_at DATETIME     DEFAULT NULL,
    created_at      DATETIME     NOT NULL,
    updated_at      DATETIME     NOT NULL,
    deleted         TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_chat_sessions_user_id (user_id),
    KEY idx_chat_sessions_last_message_at (last_message_at)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '对话会话表';

CREATE TABLE IF NOT EXISTS chat_messages (
    id           BIGINT       NOT NULL AUTO_INCREMENT,
    session_id   BIGINT       NOT NULL,
    user_id      BIGINT       NOT NULL,
    role         VARCHAR(20)  NOT NULL,
    content      TEXT         NOT NULL,
    message_type VARCHAR(30)  NOT NULL DEFAULT 'TEXT',
    metadata     TEXT         DEFAULT NULL,
    created_at   DATETIME     NOT NULL,
    updated_at   DATETIME     NOT NULL,
    deleted      TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_chat_messages_session_id (session_id),
    KEY idx_chat_messages_user_id (user_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '对话消息表';

CREATE TABLE IF NOT EXISTS projects (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    user_id     BIGINT       NOT NULL,
    name        VARCHAR(200) NOT NULL,
    description VARCHAR(1000) DEFAULT NULL,
    script      TEXT         DEFAULT NULL,
    storyboard  TEXT         DEFAULT NULL,
    created_at  DATETIME     NOT NULL,
    updated_at  DATETIME     NOT NULL,
    deleted     TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_projects_user_id (user_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '视频创作项目表';

CREATE TABLE IF NOT EXISTS video_tasks (
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    user_id       BIGINT       NOT NULL,
    project_id    BIGINT       DEFAULT NULL,
    prompt        TEXT         NOT NULL,
    params        TEXT         DEFAULT NULL,
    status        VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    progress      INT          NOT NULL DEFAULT 0,
    result_url    VARCHAR(500) DEFAULT NULL,
    error_message VARCHAR(500) DEFAULT NULL,
    created_at    DATETIME     NOT NULL,
    updated_at    DATETIME     NOT NULL,
    deleted       TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_video_tasks_user_id (user_id),
    KEY idx_video_tasks_project_id (project_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '视频生成任务表';
