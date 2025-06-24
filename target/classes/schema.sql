-- 创建论文表
CREATE TABLE IF NOT EXISTS papers (
    id VARCHAR(100) PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    abstract_text TEXT,
    publish_date DATE,
    conference VARCHAR(255),
    category VARCHAR(100),
    keywords TEXT,
    doi VARCHAR(100),
    url VARCHAR(255)
);

-- 创建论文作者关联表
CREATE TABLE IF NOT EXISTS paper_authors (
    paper_id VARCHAR(100),
    author VARCHAR(255),
    PRIMARY KEY (paper_id, author),
    FOREIGN KEY (paper_id) REFERENCES papers(id) ON DELETE CASCADE
); 