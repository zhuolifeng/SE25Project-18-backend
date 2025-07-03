"""
RAG Chatbot Service
Main FastAPI application for paper-based chatbot with RAG capabilities
"""

from fastapi import FastAPI, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from contextlib import asynccontextmanager
import uvicorn
from dotenv import load_dotenv
import os

from app.api import chat, documents, health
from app.core.config import settings
from app.core.rag_engine import RAGEngine

# Load environment variables
load_dotenv()

# Global RAG engine instance
rag_engine = None

@asynccontextmanager
async def lifespan(app: FastAPI):
    """Initialize and cleanup resources"""
    global rag_engine
    
    # Startup
    print("Initializing RAG engine...")
    rag_engine = RAGEngine()
    await rag_engine.initialize()
    
    # 设置全局RAG引擎实例到chat模块
    chat.set_rag_engine(rag_engine)
    
    print("RAG engine initialized successfully")
    
    yield
    
    # Shutdown
    print("Shutting down RAG engine...")
    await rag_engine.cleanup()
    print("RAG engine shut down successfully")

# Create FastAPI app
app = FastAPI(
    title="Paper RAG Chatbot API",
    description="A RAG-based chatbot for academic papers",
    version="1.0.0",
    lifespan=lifespan
)

# Configure CORS
app.add_middleware(
    CORSMiddleware,
    allow_origins=["http://localhost:3000", "http://localhost:8080"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# Include routers
app.include_router(health.router, prefix="/api/health", tags=["health"])
app.include_router(chat.router, prefix="/api/chat", tags=["chat"])
app.include_router(documents.router, prefix="/api/documents", tags=["documents"])

@app.get("/")
async def root():
    """Root endpoint"""
    return {
        "message": "Paper RAG Chatbot API",
        "version": "1.0.0",
        "docs": "/docs"
    }

def get_rag_engine():
    """Get the global RAG engine instance"""
    if rag_engine is None:
        raise HTTPException(status_code=500, detail="RAG engine not initialized")
    return rag_engine

if __name__ == "__main__":
    uvicorn.run(
        "app.main:app",
        host=settings.SERVICE_HOST,
        port=settings.SERVICE_PORT,
        reload=settings.DEBUG_MODE
    ) 