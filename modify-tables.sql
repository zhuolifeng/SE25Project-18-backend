USE dwp;

-- 删除依赖表的外键约束
ALTER TABLE paper_authors DROP FOREIGN KEY paper_authors_ibfk_1;
ALTER TABLE posts DROP FOREIGN KEY posts_ibfk_2;
ALTER TABLE user_view_history DROP FOREIGN KEY user_view_history_ibfk_2;

-- 清空数据
DELETE FROM paper_authors;
DELETE FROM posts WHERE paper_id IS NOT NULL;
DELETE FROM user_view_history;
DELETE FROM papers;

-- 修改papers表结构
ALTER TABLE papers 
    MODIFY COLUMN id BIGINT AUTO_INCREMENT,
    ADD COLUMN doi VARCHAR(100) UNIQUE AFTER id;

-- 重建paper_authors表
DROP TABLE paper_authors;
CREATE TABLE paper_authors (
    paper_id BIGINT,
    author VARCHAR(255),
    PRIMARY KEY (paper_id, author),
    FOREIGN KEY (paper_id) REFERENCES papers(id) ON DELETE CASCADE
);

-- 修改其他表的外键引用
ALTER TABLE posts
    MODIFY COLUMN paper_id BIGINT,
    ADD CONSTRAINT posts_paper_fk FOREIGN KEY (paper_id) REFERENCES papers(id) ON DELETE SET NULL;

ALTER TABLE user_view_history
    MODIFY COLUMN paper_id BIGINT NOT NULL,
    ADD CONSTRAINT user_view_history_paper_fk FOREIGN KEY (paper_id) REFERENCES papers(id) ON DELETE CASCADE;

-- 创建索引以提高查询性能
CREATE INDEX idx_papers_doi ON papers(doi);
CREATE INDEX idx_papers_title ON papers(title(100)); 