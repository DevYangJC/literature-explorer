-- 文献表
CREATE TABLE IF NOT EXISTS literature
(
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    original_name  VARCHAR(255) NOT NULL,
    file_path      VARCHAR(500) NOT NULL,
    file_size      BIGINT       NOT NULL,
    file_type      VARCHAR(10)  NOT NULL,
    content_length INT       DEFAULT 0,
    tags           VARCHAR(2000),
    description    VARCHAR(2000),
    reading_guide  CLOB,
    status         TINYINT   DEFAULT 1,
    create_time    TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time    TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted        TINYINT   DEFAULT 0
);

-- 问答历史表
CREATE TABLE IF NOT EXISTS qa_history
(
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    session_id      VARCHAR(64)  NOT NULL,
    literature_id   BIGINT,
    question        CLOB         NOT NULL,
    answer          CLOB         NOT NULL,
    cross_doc       BOOLEAN   DEFAULT FALSE,
    keyword         VARCHAR(200),
    session_title   VARCHAR(200),
    sequence        INT       DEFAULT 1,
    create_time     TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time     TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted         TINYINT   DEFAULT 0
);

-- 创建索引
CREATE INDEX IF NOT EXISTS idx_qa_session_id ON qa_history(session_id);
CREATE INDEX IF NOT EXISTS idx_qa_literature_id ON qa_history(literature_id);
CREATE INDEX IF NOT EXISTS idx_qa_create_time ON qa_history(create_time);
