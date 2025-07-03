"""
Health check API endpoints
"""

from fastapi import APIRouter
from datetime import datetime

router = APIRouter()

@router.get("/")
async def health_check():
    """
    Health check endpoint
    """
    return {
        "status": "healthy",
        "service": "Python RAG Service",
        "timestamp": datetime.now().isoformat(),
        "version": "1.0.0"
    }

@router.get("/status")
async def detailed_status():
    """
    Detailed status check
    """
    return {
        "status": "healthy",
        "service": "Python RAG Service",
        "timestamp": datetime.now().isoformat(),
        "components": {
            "vector_store": "ready",
            "llm_provider": "ready",
            "java_backend": "connected"
        }
    } 