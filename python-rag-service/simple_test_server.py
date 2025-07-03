#!/usr/bin/env python3
"""
简化的测试服务器 - 用于诊断FastAPI启动问题
"""

import asyncio
import uvicorn
from fastapi import FastAPI, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel
from typing import Optional
import logging

# 设置日志
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

# 创建FastAPI应用
app = FastAPI(title="Simple Test Server")

# 添加CORS
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

class ChatRequest(BaseModel):
    question: str
    user_id: Optional[str] = None

@app.get("/")
async def root():
    return {"message": "Simple test server is running"}

@app.get("/health")
async def health():
    return {"status": "healthy"}

@app.post("/test-chat")
async def test_chat(request: ChatRequest):
    """简单的测试聊天端点"""
    try:
        # 直接返回一个简单的回答，不调用LLM
        return {
            "message": f"收到问题: {request.question}",
            "answer": f"这是对'{request.question}'的测试回答",
            "sources": [],
            "question": request.question
        }
    except Exception as e:
        logger.error(f"Error in test chat: {e}")
        raise HTTPException(status_code=500, detail=str(e))

if __name__ == "__main__":
    print("启动简化的测试服务器...")
    uvicorn.run(
        "simple_test_server:app",
        host="0.0.0.0",
        port=8002,
        reload=False
    ) 