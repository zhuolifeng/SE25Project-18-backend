#!/usr/bin/env python3
"""
最小化的FastAPI服务 - 用于诊断网络问题
"""

from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
import uvicorn

# 创建FastAPI应用
app = FastAPI(title="Minimal Test Service")

# 添加CORS
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

@app.get("/")
def root():
    return {"message": "Minimal service is working!"}

@app.get("/api/health")
def health():
    return {"status": "healthy", "service": "minimal"}

@app.post("/api/chat/query")
def chat_query(request: dict):
    question = request.get("question") or request.get("message", "No question")
    return {
        "message": f"收到问题: {question}",
        "answer": f"这是对 '{question}' 的简单回答",
        "sources": [],
        "question": question
    }

if __name__ == "__main__":
    print("启动最小化测试服务...")
    uvicorn.run(app, host="0.0.0.0", port=8002, log_level="info") 