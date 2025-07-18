-- 删除数据库（如果存在）
DROP DATABASE IF EXISTS dwp;

-- 创建数据库
CREATE DATABASE dwp;

-- 使用数据库
USE dwp;

-- 创建用户表
CREATE TABLE IF NOT EXISTS users (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(50) NOT NULL UNIQUE,
    email VARCHAR(100) UNIQUE,
    phone VARCHAR(20),
    register_time TIMESTAMP,
    bio VARCHAR(500),
    avatar_url VARCHAR(255) -- 新增头像字段
);

-- 创建用户密码表
CREATE TABLE IF NOT EXISTS user_passwords (
    user_id BIGINT NOT NULL,
    password VARCHAR(255) NOT NULL,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- 创建论文表
CREATE TABLE IF NOT EXISTS papers (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    abstract_text TEXT,
    has_abstract BOOLEAN DEFAULT FALSE,
    year INT,
    journal VARCHAR(255),
    category VARCHAR(100),
    url VARCHAR(255),
    doi VARCHAR(255) UNIQUE
);

-- 创建论文作者关联表
CREATE TABLE IF NOT EXISTS paper_authors (
    paper_id BIGINT,
    author VARCHAR(255),
    PRIMARY KEY (paper_id, author),
    FOREIGN KEY (paper_id) REFERENCES papers(id) ON DELETE CASCADE
);

-- 先创建 spring_session 表
CREATE TABLE `spring_session` (
    `PRIMARY_ID` char(36) NOT NULL,
    `SESSION_ID` char(36) NOT NULL,
    `CREATION_TIME` bigint NOT NULL,
    `LAST_ACCESS_TIME` bigint NOT NULL,
    `MAX_INACTIVE_INTERVAL` int NOT NULL,
    `EXPIRY_TIME` bigint NOT NULL,
    `PRINCIPAL_NAME` varchar(100) DEFAULT NULL,
    PRIMARY KEY (`PRIMARY_ID`),
    UNIQUE KEY `SPRING_SESSION_IX1` (`SESSION_ID`),
    KEY `SPRING_SESSION_IX2` (`EXPIRY_TIME`),
    KEY `SPRING_SESSION_IX3` (`PRINCIPAL_NAME`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- 再创建 spring_session_attributes 表
CREATE TABLE `spring_session_attributes` (
    `SESSION_PRIMARY_ID` char(36) NOT NULL,
    `ATTRIBUTE_NAME` varchar(200) NOT NULL,
    `ATTRIBUTE_BYTES` blob NOT NULL,
    PRIMARY KEY (`SESSION_PRIMARY_ID`,`ATTRIBUTE_NAME`),
    CONSTRAINT `SPRING_SESSION_ATTRIBUTES_FK` 
        FOREIGN KEY (`SESSION_PRIMARY_ID`) 
        REFERENCES `spring_session` (`PRIMARY_ID`) 
        ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- 创建帖子表
CREATE TABLE IF NOT EXISTS posts (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    content TEXT,
    author_id BIGINT,
    paper_id BIGINT,
    type VARCHAR(50), -- 新增：帖子类型
    category VARCHAR(50), -- 新增：帖子分类
    status INT NOT NULL DEFAULT 1, -- 新增：帖子状态，1正常0删除
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (author_id) REFERENCES users(id) ON DELETE SET NULL,
    FOREIGN KEY (paper_id) REFERENCES papers(id) ON DELETE SET NULL
);

-- 创建点赞/点踩表
CREATE TABLE IF NOT EXISTS post_likes (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    post_id BIGINT NOT NULL,
    type TINYINT NOT NULL, -- 1=赞，-1=踩
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY unique_user_post (user_id, post_id),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (post_id) REFERENCES posts(id) ON DELETE CASCADE
);

-- 创建用户搜索历史表
CREATE TABLE IF NOT EXISTS user_search_history (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    search_text TEXT NOT NULL,  -- 使用TEXT类型存储搜索文本，支持中英文和数字
    search_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_user_search (user_id, search_time DESC)
);

-- 创建用户浏览记录表
CREATE TABLE IF NOT EXISTS user_view_history (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    paper_id BIGINT NOT NULL,
    view_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (paper_id) REFERENCES papers(id) ON DELETE CASCADE,
    INDEX idx_user_views (user_id, view_time DESC)
);

-- 创建用户收藏论文表
CREATE TABLE IF NOT EXISTS user_favorites (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    paper_id BIGINT NOT NULL,
    collect_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (paper_id) REFERENCES papers(id) ON DELETE CASCADE,
    UNIQUE KEY unique_user_paper (user_id, paper_id),
    INDEX idx_user_favorites (user_id, collect_time DESC)
); 

-- 创建用户论文标签表
CREATE TABLE IF NOT EXISTS user_paper_tags (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    paper_id BIGINT NOT NULL,
    tag_name VARCHAR(50) NOT NULL,
    tag_color VARCHAR(20),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (paper_id) REFERENCES papers(id) ON DELETE CASCADE,
    -- 确保每个用户对每篇论文的每个标签只能添加一次
    UNIQUE KEY unique_user_paper_tag (user_id, paper_id, tag_name)
);

-- 标签表
CREATE TABLE IF NOT EXISTS post_tag (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE
);

-- 帖子-标签多对多关系表
CREATE TABLE IF NOT EXISTS post_relation_tag (
    post_id BIGINT NOT NULL,
    tag_id BIGINT NOT NULL,
    PRIMARY KEY (post_id, tag_id),
    FOREIGN KEY (post_id) REFERENCES posts(id) ON DELETE CASCADE,
    FOREIGN KEY (tag_id) REFERENCES post_tag(id) ON DELETE CASCADE
);



-- 评论表
CREATE TABLE IF NOT EXISTS comment (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    content TEXT NOT NULL,
    post_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    parent_id BIGINT,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (post_id) REFERENCES posts(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (parent_id) REFERENCES comment(id) ON DELETE CASCADE
);

-- 创建帖子收藏表（与论文收藏表user_favorites分开）
CREATE TABLE IF NOT EXISTS post_favorites (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    post_id BIGINT NOT NULL,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY unique_user_post_fav (user_id, post_id),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (post_id) REFERENCES posts(id) ON DELETE CASCADE,
    INDEX idx_user_post_favorites (user_id, create_time DESC)
);


-- 创建论文引用关系表
CREATE TABLE IF NOT EXISTS paper_relations (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    source_paper_id BIGINT NOT NULL COMMENT '源论文ID（本地数据库中的论文）',
    target_paper_id BIGINT COMMENT '目标论文ID（如果在本地数据库中存在）',
    relation_type VARCHAR(20) NOT NULL COMMENT '关系类型：REFERENCES或CITED_BY',
    target_title VARCHAR(500) NOT NULL COMMENT '目标论文标题',
    target_doi VARCHAR(200) COMMENT '目标论文DOI',
    target_authors VARCHAR(1000) COMMENT '目标论文作者列表（逗号分隔）',
    target_year INT COMMENT '目标论文发表年份',
    citation_count INT COMMENT '目标论文被引用次数',
    influential_citation_count INT COMMENT '目标论文有影响力的引用次数',
    target_venue VARCHAR(200) COMMENT '目标论文发表期刊/会议',
    target_abstract VARCHAR(2000) COMMENT '目标论文摘要',
    semantic_scholar_id VARCHAR(100) COMMENT 'Semantic Scholar论文ID',
    citation_intent VARCHAR(200) COMMENT '引用意图：background,methodology,result等',
    open_access_url VARCHAR(500) COMMENT '开放访问PDF链接',
    priority_score DOUBLE COMMENT '优先级分数（用于排序）',
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (source_paper_id) REFERENCES papers(id) ON DELETE CASCADE,
    FOREIGN KEY (target_paper_id) REFERENCES papers(id) ON DELETE SET NULL,
    -- 确保同一源论文对同一目标论文的同一关系类型只能有一条记录
    UNIQUE KEY unique_source_target_doi_type (source_paper_id, target_doi, relation_type),
    INDEX idx_source_paper_type (source_paper_id, relation_type),
    INDEX idx_target_paper_type (target_paper_id, relation_type),
    INDEX idx_priority_score (priority_score DESC)
);
-- 创建用户关注表
CREATE TABLE IF NOT EXISTS user_follows (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    follower_id BIGINT NOT NULL,
    following_id BIGINT NOT NULL,
    follow_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY unique_follow_relation (follower_id, following_id),
    FOREIGN KEY (follower_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (following_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_follower (follower_id),
    INDEX idx_following (following_id)
);

-- 创建私信表
CREATE TABLE IF NOT EXISTS user_messages (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    sender_id BIGINT NOT NULL,
    receiver_id BIGINT NOT NULL,
    content TEXT NOT NULL,
    is_read BOOLEAN DEFAULT FALSE,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (sender_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (receiver_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_sender (sender_id, create_time DESC),
    INDEX idx_receiver (receiver_id, create_time DESC)
);

-- 创建会话表(用于优化会话列表查询性能)
CREATE TABLE IF NOT EXISTS message_conversations (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user1_id BIGINT NOT NULL,
    user2_id BIGINT NOT NULL,
    last_message_id BIGINT,
    last_message_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    unread_count_user1 INT DEFAULT 0,
    unread_count_user2 INT DEFAULT 0,
    FOREIGN KEY (user1_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (user2_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (last_message_id) REFERENCES user_messages(id) ON DELETE SET NULL,
    UNIQUE KEY unique_conversation (user1_id, user2_id),
    INDEX idx_user1_last_time (user1_id, last_message_time DESC),
    INDEX idx_user2_last_time (user2_id, last_message_time DESC)

);


-- 创建帖子-论文多对多关联表
CREATE TABLE IF NOT EXISTS post_related_papers (
    post_id BIGINT NOT NULL,
    paper_id BIGINT NOT NULL,
    PRIMARY KEY (post_id, paper_id),
    FOREIGN KEY (post_id) REFERENCES posts(id) ON DELETE CASCADE,
    FOREIGN KEY (paper_id) REFERENCES papers(id) ON DELETE CASCADE
);

ALTER TABLE posts ADD COLUMN views INT NOT NULL DEFAULT 0;

-- 插入10个测试用户
INSERT INTO users (id, username, email, register_time) VALUES
(1, 'user1', 'user1@example.com', NOW()),
(2, 'user2', 'user2@example.com', NOW()),
(3, 'user3', 'user3@example.com', NOW()),
(4, 'user4', 'user4@example.com', NOW()),
(5, 'user5', 'user5@example.com', NOW()),
(6, 'user6', 'user6@example.com', NOW()),
(7, 'user7', 'user7@example.com', NOW()),
(8, 'user8', 'user8@example.com', NOW()),
(9, 'user9', 'user9@example.com', NOW()),
(10, 'user10', 'user10@example.com', NOW());

-- 插入用户密码（假设密码都是"password"的加密形式）
INSERT INTO user_passwords (user_id, password) VALUES
(1, '$2a$10$xJwL5v5Jz5z5z5z5z5z5z.5z5z5z5z5z5z5z5z5z5z5z5z5z5z'),
(2, '$2a$10$xJwL5v5Jz5z5z5z5z5z5z.5z5z5z5z5z5z5z5z5z5z5z5z5z'),
(3, '$2a$10$xJwL5v5Jz5z5z5z5z5z5z.5z5z5z5z5z5z5z5z5z5z5z5z5z'),
(4, '$2a$10$xJwL5v5Jz5z5z5z5z5z5z.5z5z5z5z5z5z5z5z5z5z5z5z5z'),
(5, '$2a$10$xJwL5v5Jz5z5z5z5z5z5z.5z5z5z5z5z5z5z5z5z5z5z5z5z'),
(6, '$2a$10$xJwL5v5Jz5z5z5z5z5z5z.5z5z5z5z5z5z5z5z5z5z5z5z5z'),
(7, '$2a$10$xJwL5v5Jz5z5z5z5z5z5z.5z5z5z5z5z5z5z5z5z5z5z5z5z'),
(8, '$2a$10$xJwL5v5Jz5z5z5z5z5z5z.5z5z5z5z5z5z5z5z5z5z5z5z5z'),
(9, '$2a$10$xJwL5v5Jz5z5z5z5z5z5z.5z5z5z5z5z5z5z5z5z5z5z5z5z'),
(10, '$2a$10$xJwL5v5Jz5z5z5z5z5z5z.5z5z5z5z5z5z5z5z5z5z5z5z5z');

-- 插入10条学术帖子，每条分配不同的用户ID (1-10)
INSERT INTO posts (title, content, author_id, type, category, create_time) VALUES 
('深度学习在蛋白质结构预测中的应用', '本文探讨了深度神经网络在蛋白质三级结构预测中的最新进展，包括AlphaFold等模型的原理与突破。', 1, '论文解读', '生物', NOW()),
('量子计算对密码学的挑战', '随着量子计算的发展，传统公钥密码体制面临巨大威胁，讨论量子安全加密算法的研究现状。', 2, '研究问题', '计算机', NOW()),
('图神经网络在化学分子性质预测中的应用', '介绍图神经网络如何建模分子结构，实现分子性质的高效预测。', 3, '方法讨论', '化学', NOW()),
('黑洞信息悖论的最新进展', '综述近年来关于黑洞信息悖论的理论进展，包括全息原理和防火墙假说。', 4, '论文解读', '物理', NOW()),
('机器学习在数学猜想证明中的应用', '探讨机器学习辅助数学猜想证明的案例，如四色定理和哥德巴赫猜想。', 5, '研究问题', '数学', NOW()),
('新型二维材料的电子结构研究', '分析石墨烯等新型二维材料的电子结构及其在纳米器件中的应用前景。', 6, '论文解读', '物理', NOW()),
('基于Transformer的自然语言处理进展', '介绍Transformer架构在机器翻译、文本生成等NLP任务中的应用与优势。', 7, '方法讨论', '计算机', NOW()),
('CRISPR基因编辑技术的伦理争议', '讨论CRISPR技术在基因治疗中的应用及其引发的伦理和社会问题。', 8, '研究问题', '生物', NOW()),
('高效有机光伏材料的分子设计', '阐述有机光伏材料的分子结构优化策略及其对光电转换效率的提升作用。', 9, '方法讨论', '化学', NOW()),
('拓扑绝缘体的物理性质与应用', '介绍拓扑绝缘体的基本物理特性及其在自旋电子学中的潜在应用。', 10, '论文解读', '物理', NOW());