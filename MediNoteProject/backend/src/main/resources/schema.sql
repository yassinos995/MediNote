-- Creates Spring Boot application tables in the vital database if they do not exist.
-- This runs at startup when ddl-auto=none so Hibernate does not scan the full DB.

CREATE TABLE IF NOT EXISTS users (
    id                   BIGINT       NOT NULL AUTO_INCREMENT,
    email                VARCHAR(255) NOT NULL UNIQUE,
    password             VARCHAR(255) NOT NULL,
    full_name            VARCHAR(255) NOT NULL,
    role                 VARCHAR(50)  NOT NULL,
    enabled              TINYINT(1)   NOT NULL DEFAULT 1,
    refresh_token_hash   VARCHAR(255),
    refresh_token_expiry DATETIME(6),
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS password_reset_tokens (
    id         BIGINT       NOT NULL AUTO_INCREMENT,
    email      VARCHAR(255) NOT NULL,
    token      VARCHAR(120) NOT NULL UNIQUE,
    expires_at DATETIME(6)  NOT NULL,
    used       TINYINT(1)   NOT NULL DEFAULT 0,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS user_usage_events (
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    user_id       BIGINT       NOT NULL,
    email         VARCHAR(255) NOT NULL,
    event_type    VARCHAR(64)  NOT NULL,
    feature       VARCHAR(80),
    surface       VARCHAR(40),
    metadata_json TEXT,
    created_at    DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    INDEX idx_usage_user_created (user_id, created_at),
    INDEX idx_usage_type_created (event_type, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS user_chat_evaluations (
    id              BIGINT       NOT NULL AUTO_INCREMENT,
    user_id         BIGINT       NOT NULL,
    email           VARCHAR(255) NOT NULL,
    request_id      VARCHAR(80),
    prompt          TEXT         NOT NULL,
    relevance_score DECIMAL(4,1) NOT NULL,
    reason          VARCHAR(500),
    category        VARCHAR(80),
    routing_pipeline INT,
    status          VARCHAR(32),
    created_at      DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    INDEX idx_chat_eval_user_created (user_id, created_at),
    INDEX idx_chat_eval_request (request_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS user_engagement_scores (
    user_id              BIGINT       NOT NULL,
    email                VARCHAR(255) NOT NULL UNIQUE,
    overall_score        DECIMAL(4,1) NOT NULL DEFAULT 0.0,
    relevance_score      DECIMAL(4,1) NOT NULL DEFAULT 0.0,
    frequency_score      DECIMAL(4,1) NOT NULL DEFAULT 0.0,
    coverage_score       DECIMAL(4,1) NOT NULL DEFAULT 0.0,
    total_sessions       INT          NOT NULL DEFAULT 0,
    total_chat_questions INT          NOT NULL DEFAULT 0,
    active_days_30       INT          NOT NULL DEFAULT 0,
    features_used        VARCHAR(500) NOT NULL DEFAULT '',
    last_seen_at         DATETIME(6),
    rationale            VARCHAR(1000),
    updated_at           DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (user_id),
    INDEX idx_engagement_score (overall_score),
    INDEX idx_engagement_updated (updated_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS user_device_tokens (
    id           BIGINT       NOT NULL AUTO_INCREMENT,
    user_id      BIGINT       NOT NULL,
    email        VARCHAR(255) NOT NULL,
    platform     VARCHAR(40)  NOT NULL,
    device_id    VARCHAR(160),
    device_token VARCHAR(700) NOT NULL,
    enabled      TINYINT(1)   NOT NULL DEFAULT 1,
    created_at   DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    last_seen_at DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    UNIQUE KEY uq_device_token (device_token),
    INDEX idx_device_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS user_notifications (
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    user_id       BIGINT       NOT NULL,
    email         VARCHAR(255) NOT NULL,
    type          VARCHAR(80)  NOT NULL,
    priority      VARCHAR(20)  NOT NULL DEFAULT 'NORMAL',
    title         VARCHAR(180) NOT NULL,
    body          VARCHAR(700) NOT NULL,
    action_route  VARCHAR(120),
    metadata_json TEXT,
    read_at       DATETIME(6),
    pushed_at     DATETIME(6),
    created_at    DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    INDEX idx_notification_user_created (user_id, created_at),
    INDEX idx_notification_user_read (user_id, read_at),
    INDEX idx_notification_type_created (type, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
