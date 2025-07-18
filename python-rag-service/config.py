"""
集中配置管理
这个文件用于集中管理应用程序的配置，从环境变量中加载配置
"""

import os
import logging
from dotenv import load_dotenv

# 加载环境变量
load_dotenv()

# 应用程序配置
class Config:
    # 服务配置
    SERVICE_HOST = os.getenv("SERVICE_HOST", "0.0.0.0")
    SERVICE_PORT = int(os.getenv("SERVICE_PORT", "8002"))
    DEBUG_MODE = os.getenv("DEBUG_MODE", "true").lower() == "true"
    
    # 日志配置
    LOG_LEVEL = os.getenv("LOG_LEVEL", "INFO")
    LOG_FORMAT = '%(asctime)s - %(name)s - %(levelname)s - %(message)s'
    
    # CORS 配置
    ALLOWED_ORIGINS = os.getenv("ALLOWED_ORIGINS", "http://localhost:3000,http://localhost:8080").split(",")
    
    # Qwen API 配置
    QWEN_API_KEY = os.getenv("QWEN_API_KEY")
    
    # 向量数据库配置
    VECTOR_STORE_TYPE = os.getenv("VECTOR_STORE_TYPE", "qdrant")  # qdrant 或 chroma
    
    # Qdrant 配置
    QDRANT_HOST = os.getenv("QDRANT_HOST", "localhost")
    QDRANT_PORT = int(os.getenv("QDRANT_PORT", "6333"))
    QDRANT_API_KEY = os.getenv("QDRANT_API_KEY")
    QDRANT_COLLECTION_NAME = os.getenv("QDRANT_COLLECTION_NAME", "papers")
    
    # ChromaDB 配置
    CHROMA_PERSIST_DIRECTORY = os.getenv("CHROMA_PERSIST_DIRECTORY", "./chroma_db")
    CHROMA_COLLECTION_NAME = os.getenv("CHROMA_COLLECTION_NAME", "papers")
    
    # Java 后端集成
    JAVA_BACKEND_URL = os.getenv("JAVA_BACKEND_URL", "http://localhost:8080")
    JAVA_BACKEND_API_KEY = os.getenv("JAVA_BACKEND_API_KEY")
    
    # 嵌入模型
    EMBEDDING_MODEL = os.getenv("EMBEDDING_MODEL", "sentence-transformers/all-MiniLM-L6-v2")
    EMBEDDING_DIMENSION = int(os.getenv("EMBEDDING_DIMENSION", "384"))
    
    # LLM 设置
    LLM_PROVIDER = os.getenv("LLM_PROVIDER", "qwen")
    LLM_MODEL = os.getenv("LLM_MODEL", "qwen-turbo")
    LLM_TEMPERATURE = float(os.getenv("LLM_TEMPERATURE", "0.7"))
    LLM_MAX_TOKENS = int(os.getenv("LLM_MAX_TOKENS", "2000"))
    
    # RAG 设置
    RAG_CHUNK_SIZE = int(os.getenv("RAG_CHUNK_SIZE", "1000"))
    RAG_CHUNK_OVERLAP = int(os.getenv("RAG_CHUNK_OVERLAP", "200"))
    RAG_TOP_K = int(os.getenv("RAG_TOP_K", "5"))
    
    # PDF 提取设置
    MAX_PDF_SIZE = int(os.getenv("MAX_PDF_SIZE", "20000000"))  # 20MB

# 创建配置实例
config = Config()

# 配置日志级别
def configure_logging():
    """配置应用程序的日志级别"""
    log_level = getattr(logging, config.LOG_LEVEL.upper(), logging.INFO)
    logging.basicConfig(
        level=log_level,
        format=config.LOG_FORMAT
    )
    logging.info(f"日志级别设置为: {config.LOG_LEVEL}")

# 默认调用配置日志
configure_logging() 