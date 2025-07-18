#!/bin/bash

# 颜色定义
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

# 配置信息
SSH_USER="YOUR_SSH_USERNAME"  # 替换为您的SSH用户名
SSH_SERVER="YOUR_SERVER_IP"   # 替换为您的服务器IP
SSH_PORT="22"                 # SSH端口，默认是22
OLLAMA_PORT="11434"           # Ollama端口
WAIT_TIME=5                   # 等待SSH隧道建立的时间(秒)

echo -e "${YELLOW}=== 启动远程Ollama连接与RAG服务 ===${NC}"

# 检查SSH隧道是否已经存在
echo -e "${GREEN}检查现有SSH端口转发...${NC}"
if pgrep -f "ssh.*$OLLAMA_PORT:localhost:$OLLAMA_PORT.*$SSH_SERVER" > /dev/null; then
    echo -e "${GREEN}SSH端口转发已存在，跳过创建${NC}"
else
    echo -e "${GREEN}创建SSH端口转发: localhost:$OLLAMA_PORT -> $SSH_SERVER:$OLLAMA_PORT${NC}"
    # 启动后台SSH隧道
    ssh -N -L $OLLAMA_PORT:localhost:$OLLAMA_PORT -p $SSH_PORT $SSH_USER@$SSH_SERVER &
    SSH_PID=$!
    echo -e "${GREEN}SSH隧道进程ID: $SSH_PID${NC}"
    echo -e "${YELLOW}等待$WAIT_TIME秒，确保SSH隧道已建立...${NC}"
    sleep $WAIT_TIME
    
    # 检查SSH进程是否仍在运行
    if kill -0 $SSH_PID 2>/dev/null; then
        echo -e "${GREEN}SSH隧道已成功建立${NC}"
    else
        echo -e "${RED}SSH隧道创建失败！请检查您的SSH连接设置${NC}"
        exit 1
    fi
fi

# 创建或更新.env文件
echo -e "${GREEN}创建.env文件...${NC}"
cp env.example.ssh .env
echo -e "${GREEN}.env文件已创建/更新${NC}"

# 测试Ollama连接
echo -e "${GREEN}测试Ollama连接...${NC}"
python test_ollama.py
if [ $? -ne 0 ]; then
    echo -e "${RED}Ollama连接测试失败！请检查远程服务器上的Ollama是否正在运行${NC}"
    echo -e "${YELLOW}尝试手动验证: curl http://localhost:$OLLAMA_PORT/api/version${NC}"
    read -p "是否继续启动RAG服务? (y/n) " -n 1 -r
    echo
    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
        echo -e "${RED}已取消启动RAG服务${NC}"
        exit 1
    fi
fi

# 启动RAG服务
echo -e "${GREEN}启动RAG服务...${NC}"
python start_rag_service.py

# 清理函数 - 当脚本退出时关闭SSH隧道
cleanup() {
    echo -e "${YELLOW}清理资源...${NC}"
    if [ ! -z "$SSH_PID" ]; then
        echo -e "${GREEN}终止SSH隧道 (PID: $SSH_PID)${NC}"
        kill $SSH_PID 2>/dev/null || true
    fi
    echo -e "${GREEN}完成${NC}"
}

# 注册清理函数
trap cleanup EXIT 