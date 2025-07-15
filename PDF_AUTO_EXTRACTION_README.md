# PDF自动内容提取功能

## 功能概述
当用户查看PDF时，系统会自动将PDF内容提取并存储到Qdrant向量数据库中，使AI助手能够基于完整的论文内容回答问题。

## 实现架构

### 1. Python RAG服务 (端口8002)
- **文件**: `python-rag-service/app/services/pdf_extractor.py`
- **功能**: PDF文本提取、清理、分块、向量化
- **支持格式**: 
  - 使用pdfplumber优先提取文本
  - PyPDF2作为备选方案
  - 支持OCR处理（可扩展）

### 2. Java后端 (端口8080)
- **文件**: `src/main/java/com/dealwithpapers/dealwithpapers/service/PdfExtractionService.java`
- **功能**: PDF URL构建、调用Python服务、状态管理
- **API端点**:
  - `POST /api/papers/{id}/extract-content` - 同步提取
  - `POST /api/papers/{id}/extract-content-async` - 异步提取
  - `GET /api/papers/{id}/extract-status` - 查询状态

### 3. React前端 (端口3000)
- **文件**: `src/pages/PdfViewerPage.js`
- **功能**: PDF显示、自动触发提取、状态显示
- **用户体验**: 
  - 在PDF成功加载后自动提取内容
  - 显示提取进度和状态
  - 不影响PDF查看功能

## 使用流程

1. **用户点击"论文查看"**
   - 系统加载PDF并显示
   - 自动检查是否已提取过内容

2. **自动内容提取**
   - 如果未提取过，自动调用提取服务
   - 后台处理，不阻塞用户查看
   - 显示"正在提取PDF内容..."状态

3. **AI知识库更新**
   - Python服务下载PDF文件
   - 提取并清理文本内容
   - 分块并向量化存储到Qdrant
   - 创建可搜索的文本块

4. **状态反馈**
   - 提取完成后显示"AI知识库已准备就绪"
   - 显示创建的文本块数量
   - AI助手可以基于完整内容回答问题

## 技术特性

### 智能检测
- 避免重复提取相同PDF
- 检查PDF是否已在向量数据库中
- 支持断点续传和错误恢复

### 多格式支持
- ArXiv PDF (https://arxiv.org/pdf/xxxx.pdf)
- IEEE论文 (通过DOI转换)
- Springer、Nature等学术期刊
- 通用DOI链接处理

### 错误处理
- 网络超时和重试机制
- PDF格式兼容性处理
- 用户友好的错误提示
- 不影响PDF查看的核心功能

### 性能优化
- 异步处理，不阻塞用户界面
- 智能缓存，避免重复下载
- 分块存储，支持大文档处理
- 状态持久化和恢复

## 配置要求

### Python依赖
```bash
pip install PyPDF2 pdfplumber aiohttp sentence-transformers
```

### Java配置
```properties
# application.properties
python.rag.service.url=http://localhost:8002
```

### 服务启动顺序
1. Qdrant向量数据库 (端口6333)
2. Python RAG服务 (端口8002)
3. Java后端服务 (端口8080)
4. React前端服务 (端口3000)

## 测试验证

### 基本功能测试
1. 启动所有服务
2. 搜索一篇论文（如"Attention Is All You Need"）
3. 点击"论文查看"
4. 观察PDF加载和内容提取状态

### 验证标准
- ✅ PDF正常显示
- ✅ 显示"正在提取PDF内容..."状态
- ✅ 5-10秒后显示"AI知识库已准备就绪"
- ✅ AI助手可以回答PDF内容相关问题
- ✅ 重复查看同一PDF时不再提取

### 错误处理测试
- 网络断开时的处理
- 无效PDF链接的处理
- Python服务不可用时的处理
- 大文件超时的处理

## 故障排除

### 常见问题
1. **提取状态一直显示"正在提取"**
   - 检查Python RAG服务是否运行
   - 检查Qdrant数据库连接
   - 查看后端日志中的错误信息

2. **PDF加载失败**
   - 检查PDF URL是否有效
   - 尝试不同的学术论文源
   - 查看网络连接和代理设置

3. **AI助手无法获取PDF内容**
   - 确认向量数据库中有对应数据
   - 检查文本分块是否成功
   - 验证检索功能是否正常

### 日志查看
- **Python服务**: `python-rag-service/logs/`
- **Java后端**: 控制台输出和日志文件
- **前端**: 浏览器开发者工具控制台

## 扩展功能

### 未来改进
- OCR支持扫描版PDF
- 表格和图像内容提取
- 多语言文档支持
- 批量提取功能
- 提取质量评估

### 自定义配置
- 文本块大小调整
- 向量维度配置
- 提取超时设置
- 缓存策略优化