-- 创建数据库（如果不存在）
CREATE DATABASE IF NOT EXISTS dwp;

-- 使用数据库
USE dwp;

-- 创建用户表
CREATE TABLE IF NOT EXISTS users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    email VARCHAR(100) UNIQUE,
    phone VARCHAR(20),
    register_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 创建用户密码表
CREATE TABLE IF NOT EXISTS user_passwords (
    user_id BIGINT PRIMARY KEY,
    password VARCHAR(255) NOT NULL,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- 创建论文表
CREATE TABLE IF NOT EXISTS papers (
    id VARCHAR(100) PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    abstract_text TEXT,
    publish_date DATE,
    conference VARCHAR(255),
    category VARCHAR(100),
    url VARCHAR(255)
);

-- 创建论文作者关联表
CREATE TABLE IF NOT EXISTS paper_authors (
    paper_id VARCHAR(100),
    author VARCHAR(255),
    PRIMARY KEY (paper_id, author),
    FOREIGN KEY (paper_id) REFERENCES papers(id) ON DELETE CASCADE
);

-- 创建Spring Session表（如果使用JDBC存储Session）
CREATE TABLE IF NOT EXISTS SPRING_SESSION (
    PRIMARY_ID CHAR(36) NOT NULL,
    SESSION_ID CHAR(36) NOT NULL,
    CREATION_TIME BIGINT NOT NULL,
    LAST_ACCESS_TIME BIGINT NOT NULL,
    MAX_INACTIVE_INTERVAL INT NOT NULL,
    EXPIRY_TIME BIGINT NOT NULL,
    PRINCIPAL_NAME VARCHAR(100),
    CONSTRAINT SPRING_SESSION_PK PRIMARY KEY (PRIMARY_ID)
);

CREATE TABLE IF NOT EXISTS SPRING_SESSION_ATTRIBUTES (
    SESSION_PRIMARY_ID CHAR(36) NOT NULL,
    ATTRIBUTE_NAME VARCHAR(200) NOT NULL,
    ATTRIBUTE_BYTES BLOB NOT NULL,
    CONSTRAINT SPRING_SESSION_ATTRIBUTES_PK PRIMARY KEY (SESSION_PRIMARY_ID, ATTRIBUTE_NAME),
    CONSTRAINT SPRING_SESSION_ATTRIBUTES_FK FOREIGN KEY (SESSION_PRIMARY_ID) REFERENCES SPRING_SESSION(PRIMARY_ID) ON DELETE CASCADE
);

-- 创建索引（不使用IF NOT EXISTS语法）
-- 先检查索引是否存在，如果不存在则创建
DROP PROCEDURE IF EXISTS create_index_if_not_exists;

DELIMITER //
CREATE PROCEDURE create_index_if_not_exists()
BEGIN
    DECLARE index_exists INT;
    
    -- 检查SPRING_SESSION_IX1索引是否存在
    SELECT COUNT(1) INTO index_exists
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
    AND table_name = 'SPRING_SESSION'
    AND index_name = 'SPRING_SESSION_IX1';
    
    IF index_exists = 0 THEN
        CREATE INDEX SPRING_SESSION_IX1 ON SPRING_SESSION (SESSION_ID);
    END IF;
    
    -- 检查SPRING_SESSION_IX2索引是否存在
    SELECT COUNT(1) INTO index_exists
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
    AND table_name = 'SPRING_SESSION'
    AND index_name = 'SPRING_SESSION_IX2';
    
    IF index_exists = 0 THEN
        CREATE INDEX SPRING_SESSION_IX2 ON SPRING_SESSION (EXPIRY_TIME);
    END IF;
    
    -- 检查SPRING_SESSION_IX3索引是否存在
    SELECT COUNT(1) INTO index_exists
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
    AND table_name = 'SPRING_SESSION'
    AND index_name = 'SPRING_SESSION_IX3';
    
    IF index_exists = 0 THEN
        CREATE INDEX SPRING_SESSION_IX3 ON SPRING_SESSION (PRINCIPAL_NAME);
    END IF;
END //
DELIMITER ;

-- 执行存储过程
CALL create_index_if_not_exists();

-- 删除存储过程
DROP PROCEDURE IF EXISTS create_index_if_not_exists; 