"""
Configuration management for the RAG service
"""

from pydantic_settings import BaseSettings
from typing import Optional
import os

class Settings(BaseSettings):
    """Application settings"""
    
    # LLM Configuration - 从环境变量读取，不使用硬编码
    QWEN_API_KEY: Optional[str] = None
    
    # Vector Database Configuration
    QDRANT_HOST: str = "localhost"
    QDRANT_PORT: int = 6333
    QDRANT_API_KEY: Optional[str] = None
    QDRANT_COLLECTION_NAME: str = "papers"
    
    # ChromaDB Configuration
    CHROMA_PERSIST_DIRECTORY: str = "./chroma_db"
    CHROMA_COLLECTION_NAME: str = "papers"
    
    # Java Backend Integration
    JAVA_BACKEND_URL: str = "http://localhost:8080"
    JAVA_BACKEND_API_KEY: Optional[str] = None
    
    # Embedding Model
    EMBEDDING_MODEL: str = "sentence-transformers/all-MiniLM-L6-v2"
    EMBEDDING_DIMENSION: int = 384
    
    # Qwen LLM Settings
    LLM_PROVIDER: str = "qwen"  # 只支持qwen
    LLM_MODEL: str = "qwen-turbo"  # qwen-turbo, qwen-plus, qwen-max
    LLM_TEMPERATURE: float = 0.7
    LLM_MAX_TOKENS: int = 2000
    
    # RAG Settings
    RAG_CHUNK_SIZE: int = 1000
    RAG_CHUNK_OVERLAP: int = 200
    RAG_TOP_K: int = 5
    
    # Service Configuration
    SERVICE_HOST: str = "0.0.0.0"
    SERVICE_PORT: int = 8002
    DEBUG_MODE: bool = True
    
    # Vector Store Type
    VECTOR_STORE_TYPE: str = "qdrant"  # qdrant or chroma
    
    class Config:
        env_file = ".env"
        env_file_encoding = 'utf-8'
        case_sensitive = True

# Create settings instance
settings = Settings() 