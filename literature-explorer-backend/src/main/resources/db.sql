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
