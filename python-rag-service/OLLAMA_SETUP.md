# 使用本地Ollama模型配置指南

本文档指导您如何配置RAG服务使用本地Ollama模型，替代默认的通义千问(Qwen)云服务。

## Ollama 设置指南

### 本地安装Ollama

1. 在[Ollama官网](https://ollama.com)下载并安装Ollama
2. 启动Ollama服务
3. 拉取一个模型(例如 llama2):
   ```bash
   ollama pull llama2
   ```
4. 确认模型已拉取:
   ```bash
   ollama list
   ```

### 配置项目使用Ollama

1. 确保您已经安装了必要的依赖:
   ```bash
   pip install -r requirements.txt
   ```

2. 创建`.env`文件，配置LLM提供商为Ollama:
   ```
   LLM_PROVIDER=ollama
   LLM_MODEL=llama2
   OLLAMA_BASE_URL=http://localhost:11434
   OLLAMA_TIMEOUT=120
   ```

3. 测试Ollama连接:
   ```bash
   python test_ollama.py
   ```

4. 启动RAG服务:
   ```bash
   python start_rag_service.py
   ```

### 远程Ollama服务器设置

如果您想在远程服务器上运行Ollama，有两种方法可以连接：

#### 方法1：直接连接远程Ollama（需要公共IP）

1. 在远程服务器上安装并启动Ollama:
   ```bash
   # 连接到服务器
   ssh user@your-server-ip
   
   # 安装Ollama
   curl -fsSL https://ollama.com/install.sh | sh
   
   # 启动Ollama服务（监听所有IP）
   OLLAMA_HOST=0.0.0.0:11434 ollama serve
   
   # 在另一个终端中拉取模型
   ollama pull llama2
   ```

2. 确保服务器防火墙允许11434端口:
   ```bash
   sudo ufw allow 11434/tcp
   ```

3. 在本地项目中，创建`.env`文件:
   ```
   LLM_PROVIDER=ollama
   LLM_MODEL=llama2
   OLLAMA_BASE_URL=http://your-server-ip:11434
   OLLAMA_TIMEOUT=120
   ```

#### 方法2：通过SSH隧道连接（更安全）

1. 在远程服务器上设置Ollama:
   ```bash
   # 连接到服务器
   ssh user@your-server-ip
   
   # 安装Ollama
   curl -fsSL https://ollama.com/install.sh | sh
   
   # 启动Ollama服务（只监听localhost）
   ollama serve
   
   # 在另一个终端中拉取模型
   ollama pull llama2
   ```

2. 使用SSH端口转发:
   ```bash
   # 在本地机器上运行
   ssh -N -L 11434:localhost:11434 user@your-server-ip
   ```

3. 在本地项目中，创建`.env`文件:
   ```
   LLM_PROVIDER=ollama
   LLM_MODEL=llama2
   OLLAMA_BASE_URL=http://localhost:11434
   OLLAMA_TIMEOUT=120
   ```

4. 使用提供的自动化脚本:
   ```bash
   # 首先编辑脚本，设置您的SSH连接信息
   vi start_with_remote_ollama.sh
   
   # 设置脚本执行权限
   chmod +x start_with_remote_ollama.sh
   
   # 运行脚本
   ./start_with_remote_ollama.sh
   ```

### 常见问题排查

1. **连接超时**
   
   检查Ollama服务是否在远程服务器上运行：
   ```bash
   # 在服务器上
   ps aux | grep ollama
   ```

2. **模型不可用**

   检查模型是否已在服务器上安装：
   ```bash
   # 在服务器上
   ollama list
   ```

3. **端口转发问题**

   检查端口转发是否成功：
   ```bash
   # 在本地机器上
   netstat -an | grep 11434
   ```

4. **手动测试API**

   使用curl测试API连接：
   ```bash
   # 在本地机器上
   curl http://localhost:11434/api/version
   ``` 