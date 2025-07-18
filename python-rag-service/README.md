# Python RAG Service for Paper Management System

这是一个基于Python的RAG（Retrieval-Augmented Generation）服务，为论文管理系统提供智能聊天机器人功能。

## 架构概览

```
┌─────────────┐     ┌────────────────┐     ┌─────────────────┐
│   React     │────▶│ Java Spring    │────▶│  Python RAG     │
│  Frontend   │     │   Backend      │     │    Service      │
└─────────────┘     └────────────────┘     └─────────────────┘
                            │                        │
                            ▼                        ▼
                    ┌────────────┐          ┌────────────────┐
                    │   MySQL    │          │  Vector DB     │
                    │  Database  │          │(Qdrant/Chroma) │
                    └────────────┘          └────────────────┘
```

## 技术栈

- **框架**: FastAPI
- **LLM框架**: LangChain
- **向量数据库**: Qdrant / ChromaDB
- **LLM提供商**: 
  - **阿里云通义千问(Qwen)** - 默认提供商
  - **本地Ollama** - 支持本地部署的大型语言模型
- **嵌入模型**: Sentence Transformers
- **异步处理**: asyncio

## 项目结构

```
python-rag-service/
├── app/
│   ├── __init__.py
│   ├── main.py                 # FastAPI应用入口
│   ├── api/                    # API路由
│   │   ├── __init__.py
│   │   ├── chat.py            # 聊天相关端点
│   │   ├── documents.py       # 文档管理端点
│   │   └── health.py          # 健康检查端点
│   ├── core/                   # 核心业务逻辑
│   │   ├── __init__.py
│   │   ├── config.py          # 配置管理
│   │   ├── rag_engine.py      # RAG引擎
│   │   ├── vector_store.py    # 向量存储管理
│   │   └── llm_provider.py    # LLM提供商管理
│   ├── integrations/           # 外部集成
│   │   ├── __init__.py
│   │   └── java_backend.py    # Java后端集成
│   └── utils/                  # 工具函数
│       ├── __init__.py
│       └── document_processor.py
├── tests/                      # 测试文件
├── requirements.txt            # Python依赖
├── .env.example               # 环境变量示例
├── OLLAMA_SETUP.md            # Ollama配置指南
├── Dockerfile                 # Docker配置
└── README.md                  # 本文件
```

## 安装和配置

### 1. 克隆项目并进入目录
```bash
cd python-rag-service
```

### 2. 使用自动依赖检查脚本
```bash
python check_and_install_dependencies.py
```
这个脚本将自动检查所有必要的依赖项，并在需要时安装它们。

### 3. 配置环境变量
```bash
cp .env.example .env
# 编辑 .env 文件，填入必要的配置
```

### 4. 启动向量数据库

**使用Qdrant:**
```bash
docker run -p 6333:6333 qdrant/qdrant
```

**使用ChromaDB:**
ChromaDB会自动在本地创建持久化目录。

### 5. 运行服务
```bash
# 使用增强的启动脚本（包含依赖检查）
python start_rag_service.py

# 跳过依赖检查
python start_rag_service.py --skip-deps-check

# 直接使用uvicorn启动
python -m app.main

# 或使用uvicorn命令
uvicorn app.main:app --reload --port 8002
```

## API使用示例

### 1. 健康检查
```bash
curl http://localhost:8002/api/health
```

### 2. 处理论文
```bash
curl -X POST http://localhost:8002/api/documents/process \
  -H "Content-Type: application/json" \
  -d '{
    "paper_ids": ["paper001", "paper002"]
  }'
```

### 3. 聊天查询
```bash
curl -X POST http://localhost:8002/api/chat/query \
  -H "Content-Type: application/json" \
  -d '{
    "question": "机器学习在自然语言处理中有哪些应用？",
    "user_id": "user123"
  }'
```

## 与Java后端集成

Python RAG服务通过REST API与Java后端通信：

1. **获取论文数据**: 从Java后端获取论文详情
2. **记录用户行为**: 将搜索历史发送到Java后端
3. **用户认证**: 验证用户身份（可选）

## 配置说明

### LLM配置
- **Qwen (通义千问)** - 默认提供商，使用阿里云API
  ```
  LLM_PROVIDER=qwen
  LLM_MODEL=qwen-turbo
  QWEN_API_KEY=your_api_key
  ```

- **Ollama** - 本地部署的开源大型语言模型
  ```
  LLM_PROVIDER=ollama
  LLM_MODEL=llama2  # 或其他已下载的模型
  OLLAMA_BASE_URL=http://localhost:11434
  ```
  详细配置参见 [OLLAMA_SETUP.md](./OLLAMA_SETUP.md)

### 向量数据库配置
- 支持Qdrant和ChromaDB
- 可配置集合名称、持久化路径等

### RAG配置
- 文档分块大小和重叠
- 检索文档数量（top_k）
- 嵌入模型选择

## 开发指南

### 添加新的LLM提供商
1. 在 `app/core/llm_provider.py` 中添加新的提供商类
2. 更新配置以支持新提供商
3. 在环境变量中添加必要的API密钥

### 自定义文档处理
1. 修改 `app/utils/document_processor.py`
2. 实现自定义的文档分割逻辑
3. 添加元数据提取功能

## 使用本地Ollama模型

本项目支持使用本地部署的Ollama模型，无需云服务API密钥：

1. **安装Ollama**
   - [下载并安装](https://ollama.com/download) Ollama
   - 使用 `ollama pull llama2` 下载模型

2. **配置RAG服务**
   - 编辑 `.env` 文件，设置 `LLM_PROVIDER=ollama`
   - 指定要使用的模型，如 `LLM_MODEL=llama2`

3. **测试Ollama连接**
   ```bash
   python test_ollama.py
   ```

4. **启动服务**
   ```bash
   python start_rag_service.py
   ```

更详细的Ollama配置说明请参见 [OLLAMA_SETUP.md](./OLLAMA_SETUP.md)。

## 性能优化

1. **异步处理**: 使用asyncio处理并发请求
2. **批量处理**: 支持批量添加文档到向量数据库
3. **缓存**: 可以添加Redis缓存常见查询
4. **连接池**: 复用数据库连接
5. **本地模型优化**: 
   - 对于Ollama，使用较小模型（如gemma:2b）提高响应速度
   - 确保有足够的GPU内存或系统内存

## 部署

### Docker部署
```bash
docker build -t paper-rag-service .
docker run -p 8002:8002 --env-file .env paper-rag-service
```

### 生产环境建议
1. 使用Gunicorn或Uvicorn workers
2. 配置反向代理（Nginx）
3. 设置适当的资源限制
4. 启用日志和监控

## 故障排除

### 常见问题

1. **向量数据库连接失败**
   - 检查向量数据库是否运行
   - 验证连接配置

2. **LLM API错误**
   - 检查API密钥是否正确
   - 验证网络连接

3. **Ollama连接问题**
   - 确认Ollama服务正在运行（`ollama ps`）
   - 检查OLLAMA_BASE_URL配置是否正确

4. **内存不足**
   - 减小批处理大小
   - 使用更小的嵌入或语言模型

## 贡献指南

1. Fork项目
2. 创建功能分支
3. 提交更改
4. 创建Pull Request

## 许可证

[您的许可证信息] 