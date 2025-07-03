# Qwen（通义千问）API 集成设置指南

本文档详细说明如何在RAG系统中集成和使用阿里云通义千问（Qwen）API。

## 支持的LLM提供商

目前只支持：
- **Qwen API**: 阿里云通义千问模型

## Qwen API配置

### 1. 获取API密钥
1. 访问[阿里云灵积控制台](https://dashscope.console.aliyun.com/)
2. 开通通义千问服务
3. 创建并获取API Key

### 2. 配置环境变量
```bash
# .env 文件
LLM_PROVIDER=qwen
QWEN_API_KEY=your_dashscope_api_key
LLM_MODEL=qwen-turbo  # 或 qwen-plus, qwen-max
```

### 3. 可用模型
- `qwen-turbo`: 快速版本，响应速度最快，成本最低
- `qwen-plus`: 平衡版本，推荐日常使用
- `qwen-max`: 最强版本，效果最好，成本最高

## 使用示例

### 启动服务
```bash
cd python-rag-service
python -m app.main
```

### 测试集成
```bash
python test_qwen_integration.py
```

### API调用示例
```python
import requests

# 测试聊天功能
response = requests.post("http://localhost:8002/api/chat/query", json={
    "question": "请介绍一下机器学习的基本概念",
    "user_id": "test_user"
})

print(response.json())
```

## 模型特性对比

| 模型 | 响应速度 | 成本 | 中文支持 | 推理能力 | 适用场景 |
|------|----------|------|----------|----------|----------|
| qwen-turbo | 最快 | 最低 | 优秀 | 良好 | 日常问答、快速响应 |
| qwen-plus | 快 | 中等 | 优秀 | 很好 | 推荐使用，平衡性能 |
| qwen-max | 中等 | 较高 | 优秀 | 最强 | 复杂分析、高质量要求 |

## 故障排除

### 常见问题

#### 1. API密钥错误
```
错误: QWEN_API_KEY is required for Qwen provider
解决: 检查.env文件中的API密钥是否正确设置
```

#### 2. 网络连接问题
```
错误: Connection timeout
解决: 检查网络连接，确保可以访问阿里云API服务
```

#### 3. 模型不存在
```
错误: Model not found
解决: 检查模型名称是否正确，支持: qwen-turbo, qwen-plus, qwen-max
```

#### 4. 依赖包未安装
```
错误: No module named 'dashscope'
解决: pip install dashscope
```

### 日志调试
```bash
# 开启详细日志
DEBUG_MODE=true python -m app.main
```

## 模型选择建议

### 开发测试环境
```bash
LLM_PROVIDER=qwen
QWEN_API_KEY=your_api_key
LLM_MODEL=qwen-turbo
```

### 生产环境（推荐）
```bash
LLM_PROVIDER=qwen
QWEN_API_KEY=your_api_key
LLM_MODEL=qwen-plus
```

### 高质量要求场景
```bash
LLM_PROVIDER=qwen
QWEN_API_KEY=your_api_key
LLM_MODEL=qwen-max
```

## 安全注意事项

1. **API密钥安全**: 不要将API密钥提交到代码仓库
2. **网络安全**: 生产环境使用HTTPS
3. **访问控制**: 设置适当的用户认证和授权
4. **数据隐私**: 注意用户数据的处理和存储

## 成本优化

1. **选择合适模型**: 根据场景选择性价比最优的模型
2. **控制token数量**: 设置合理的max_tokens参数
3. **缓存机制**: 对相似问题实现缓存避免重复调用
4. **监控使用量**: 定期检查API使用统计

## API限制

- **请求频率**: 根据套餐不同有相应限制
- **并发请求**: 建议控制并发数量
- **Token限制**: 单次请求有最大token限制

## 技术支持

如有问题，可以：
1. 查看[阿里云文档](https://help.aliyun.com/zh/dashscope/)
2. 联系阿里云技术支持
3. 查看项目README和代码注释 