"""
RAG Chatbot Service
Main FastAPI application for paper-based chatbot with RAG capabilities
"""

import logging
import os
from fastapi import FastAPI, Request, Depends, HTTPException
from fastapi.responses import JSONResponse
from fastapi.middleware.cors import CORSMiddleware
from contextlib import asynccontextmanager
import uvicorn
import sys
from typing import Optional

from app.core.rag_engine import RAGEngine
from app.api import chat, documents, health
from config import config

# 获取根日志记录器
logger = logging.getLogger("rag_service")

# 全局RAG引擎实例
_rag_engine: Optional[RAGEngine] = None

# 配置详细的日志记录
def configure_logging():
    # 获取日志级别
    log_level_name = os.getenv("LOG_LEVEL", "DEBUG")  # 默认使用DEBUG级别用于调试
    log_level = getattr(logging, log_level_name.upper(), logging.INFO)
    
    # 确保使用绝对路径
    log_file_path = os.path.abspath("rag_service_debug.log")
    
    # 检查是否已配置过日志
    if len(logging.root.handlers) > 0:
        print(f"日志已配置，添加文件处理器到: {log_file_path}")
        try:
            file_handler = logging.FileHandler(log_file_path, mode='a')
            file_handler.setFormatter(logging.Formatter('%(asctime)s - %(name)s - %(levelname)s - %(message)s'))
            logging.root.addHandler(file_handler)
        except Exception as e:
            print(f"添加日志文件处理器失败: {str(e)}")
        return
    
    # 基本配置
    try:
        print(f"配置日志系统，日志级别: {log_level_name}, 文件: {log_file_path}")
        logging.basicConfig(
            level=log_level,
            format='%(asctime)s - %(name)s - %(levelname)s - %(message)s',
            datefmt='%Y-%m-%d %H:%M:%S',
            handlers=[
                logging.StreamHandler(sys.stdout),  # 输出到控制台
                logging.FileHandler(log_file_path, mode='a')  # 同时写入文件，追加模式
            ]
        )
        
        # 设置一些库的日志级别更高，以减少噪音
        logging.getLogger("httpx").setLevel(logging.WARNING)
        logging.getLogger("httpcore").setLevel(logging.WARNING)
        logging.getLogger("asyncio").setLevel(logging.WARNING)
        logging.getLogger("uvicorn").setLevel(logging.INFO)
        
        # 设置我们自己的模块为DEBUG
        logging.getLogger("app").setLevel(logging.DEBUG)
        
        # 确认日志配置成功
        root_logger = logging.getLogger()
        handlers_info = [f"{h.__class__.__name__} -> {getattr(h, 'baseFilename', 'stream')}" for h in root_logger.handlers]
        print(f"日志系统已配置，级别: {log_level_name}, 处理器: {handlers_info}")
        
        # 测试日志
        logging.info("========= 日志系统初始化成功 =========")
    except Exception as e:
        print(f"配置日志系统失败: {str(e)}")
        import traceback
        print(traceback.format_exc())

# 应用启动时的初始化
@asynccontextmanager
async def lifespan(app: FastAPI):
    # 启动前：初始化RAG引擎
    configure_logging()  # 启动时配置日志系统
    
    logging.info("正在初始化RAG服务...")
    global _rag_engine
    _rag_engine = RAGEngine()
    await _rag_engine.initialize()
    
    # 设置RAG引擎到chat模块
    from app.api import chat
    chat.set_rag_engine(_rag_engine)
    logging.info("RAG服务初始化完成并已设置到chat模块!")
    
    yield  # 服务运行中
    
    # 关闭时：清理资源
    logging.info("正在关闭RAG服务...")
    if _rag_engine:
        await _rag_engine.cleanup()
    logging.info("RAG服务已关闭!")

# 创建FastAPI应用
app = FastAPI(
    title="Python RAG Service",
    description="RAG服务用于支持Deal With Papers的智能问答功能",
    version="0.1.0",
    lifespan=lifespan
)

# 配置CORS
app.add_middleware(
    CORSMiddleware,
    allow_origins=config.ALLOWED_ORIGINS,
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# 包含路由
app.include_router(health.router, prefix="/api/health", tags=["health"])
app.include_router(chat.router, prefix="/api/chat", tags=["chat"])
app.include_router(documents.router, prefix="/api/documents", tags=["documents"])

@app.get("/")
async def root():
    """根端点"""
    return {
        "message": "Paper RAG Chatbot API",
        "version": "1.0.0",
        "docs": "/docs"
    }

def get_rag_engine():
    """获取全局RAG引擎实例"""
    if _rag_engine is None:
        logger.error("尝试访问未初始化的RAG引擎")
        raise HTTPException(status_code=500, detail="RAG引擎未初始化")
    return _rag_engine

if __name__ == "__main__":
    logger.info(f"启动服务于 {config.SERVICE_HOST}:{config.SERVICE_PORT} (Debug模式: {config.DEBUG_MODE})")
    uvicorn.run(
        "app.main:app",
        host=config.SERVICE_HOST,
        port=config.SERVICE_PORT,
        reload=config.DEBUG_MODE
    ) 