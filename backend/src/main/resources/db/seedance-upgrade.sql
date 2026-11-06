-- 已有数据库升级脚本。新数据库直接使用 schema.sql，无需执行本文件。
ALTER TABLE video_tasks
    ADD COLUMN chat_session_id BIGINT DEFAULT NULL,
    ADD COLUMN chat_message_id BIGINT DEFAULT NULL,
    ADD COLUMN provider VARCHAR(32) NOT NULL DEFAULT 'MOCK',
    ADD COLUMN provider_task_id VARCHAR(128) DEFAULT NULL,
    ADD COLUMN provider_status VARCHAR(32) DEFAULT NULL,
    ADD COLUMN provider_model VARCHAR(128) DEFAULT NULL,
    ADD COLUMN provider_error_code VARCHAR(100) DEFAULT NULL,
    ADD COLUMN provider_result_url VARCHAR(1500) DEFAULT NULL,
    ADD COLUMN storage_key VARCHAR(800) DEFAULT NULL,
    ADD COLUMN client_request_id VARCHAR(100) DEFAULT NULL,
    ADD COLUMN completion_tokens BIGINT DEFAULT NULL,
    ADD COLUMN total_tokens BIGINT DEFAULT NULL,
    ADD COLUMN unit_price_cny_per_million DECIMAL(12,4) DEFAULT NULL,
    ADD COLUMN pricing_version VARCHAR(50) DEFAULT NULL,
    ADD COLUMN estimated_cost_cny DECIMAL(18,6) DEFAULT NULL,
    ADD COLUMN currency VARCHAR(8) DEFAULT 'CNY',
    ADD COLUMN submitted_at DATETIME DEFAULT NULL,
    ADD COLUMN last_polled_at DATETIME DEFAULT NULL,
    ADD COLUMN next_poll_at DATETIME DEFAULT NULL,
    ADD COLUMN completed_at DATETIME DEFAULT NULL,
    ADD COLUMN lease_until DATETIME DEFAULT NULL,
    ADD COLUMN attempt_count INT NOT NULL DEFAULT 0,
    ADD COLUMN version INT NOT NULL DEFAULT 0,
    MODIFY COLUMN result_url VARCHAR(1000) DEFAULT NULL,
    MODIFY COLUMN error_message VARCHAR(1000) DEFAULT NULL;

ALTER TABLE video_tasks
    ADD UNIQUE KEY uk_video_tasks_user_request (user_id, client_request_id),
    ADD UNIQUE KEY uk_video_tasks_provider_task (provider, provider_task_id),
    ADD KEY idx_video_tasks_chat_message_id (chat_message_id),
    ADD KEY idx_video_tasks_due (status, next_poll_at);
