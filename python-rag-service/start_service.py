#!/usr/bin/env python3
"""
简化的RAG服务启动脚本
避免复杂的初始化过程，专注于基本功能
"""

import asyncio
import uvicorn
from fastapi import FastAPI, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel
from typing import Optional, List, Dict, Any
import logging
import os
from dotenv import load_dotenv

# 加载环境变量
load_dotenv()

# 设置日志
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

# 创建FastAPI应用
app = FastAPI(title="Simple RAG Service")

# 添加CORS
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

class ChatRequest(BaseModel):
    question: Optional[str] = None
    message: Optional[str] = None
    user_id: Optional[str] = None
    paper_id: Optional[str] = None
    session_id: Optional[str] = None
    history: Optional[List[Dict[str, str]]] = []

class ChatResponse(BaseModel):
    message: str
    answer: Optional[str] = None
    sources: List[Dict[str, Any]]
    question: str

@app.get("/")
async def root():
    return {"message": "Simple RAG Service is running"}

@app.get("/api/health")
async def health():
    return {
        "status": "healthy",
        "service": "Simple RAG Service",
        "message": "Service is ready for chat"
    }

@app.post("/api/chat/query", response_model=ChatResponse)
async def chat_query(request: ChatRequest):
    """简化的聊天查询端点"""
    try:
        # 提取问题
        question = request.question or request.message
        if not question:
            raise HTTPException(status_code=400, detail="No question provided")
        
        logger.info(f"Processing query: {question[:50]}...")
        
        # 模拟AI回答（不调用真实的LLM）
        if "你好" in question or "hello" in question.lower():
            answer = "你好！我是论文管理系统的AI助手。我可以帮助你查询和分析论文信息。"
        elif "机器学习" in question or "machine learning" in question.lower():
            answer = "机器学习是人工智能的一个分支，它使计算机能够在没有明确编程的情况下学习和改进。"
        elif "人工智能" in question or "ai" in question.lower():
            answer = "人工智能是计算机科学的一个分支，旨在创建能够执行通常需要人类智能的任务的系统。"
        else:
            answer = f"这是一个关于'{question}'的模拟回答。在实际部署中，这里会调用Qwen API生成真实的回答。"
        
        # 模拟来源
        sources = [
            {
                "paper_id": "demo_paper_001",
                "title": "示例论文标题",
                "authors": "示例作者",
                "chunk": "这是从示例论文中提取的相关内容片段..."
            }
        ]
        
        response = ChatResponse(
            message=answer,
            answer=answer,
            sources=sources,
            question=question
        )
        
        return response
        
    except Exception as e:
        logger.error(f"Error processing chat query: {str(e)}")
        raise HTTPException(status_code=500, detail=str(e))

@app.get("/api/documents/status")
async def document_status():
    """文档处理状态"""
    return {
        "message": "Document processing service is ready",
        "status": "ready"
    }

if __name__ == "__main__":
    print("启动简化的RAG服务...")
    print("这个服务提供基本的聊天功能，用于测试前端集成")
    print("访问 http://localhost:8002/docs 查看API文档")
    
    uvicorn.run(
        "start_service:app",
        host="0.0.0.0",
        port=8002,
        reload=False
    ) 