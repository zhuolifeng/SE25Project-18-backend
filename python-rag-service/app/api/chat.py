"""
Chat API endpoints
"""

from fastapi import APIRouter, HTTPException, Depends
from pydantic import BaseModel
from typing import List, Optional, Dict, Any
import logging

from app.core.rag_engine import RAGEngine

logger = logging.getLogger(__name__)

router = APIRouter()

class HistoryItem(BaseModel):
    """Chat history item"""
    role: str  # 'user' or 'assistant'
    content: str

class ChatRequest(BaseModel):
    """Chat request model - matches frontend format"""
    message: Optional[str] = None  # Support legacy 'message' field
    question: Optional[str] = None  # Support new 'question' field
    user_id: Optional[str] = None
    paper_id: Optional[str] = None
    conversation_id: Optional[str] = None
    session_id: Optional[str] = None  # Alternative to conversation_id
    history: Optional[List[HistoryItem]] = []

class ChatResponse(BaseModel):
    """Chat response model - matches frontend expectations"""
    message: str  # Frontend expects 'message' field
    answer: Optional[str] = None  # Keep 'answer' for backward compatibility
    sources: List[Dict[str, Any]]
    question: str
    conversation_id: Optional[str] = None

class ConversationClearRequest(BaseModel):
    """Request to clear conversation"""
    session_id: Optional[str] = None
    conversation_id: Optional[str] = None

# 全局RAG引擎实例变量
_rag_engine = None

def get_rag_engine():
    """获取全局RAG引擎实例"""
    if _rag_engine is None:
        raise HTTPException(status_code=500, detail="RAG engine not initialized")
    return _rag_engine

def set_rag_engine(engine: RAGEngine):
    """设置全局RAG引擎实例"""
    global _rag_engine
    _rag_engine = engine
    logging.info("RAG引擎已成功设置到chat模块")

@router.post("/query", response_model=ChatResponse)
async def chat_query(
    request: ChatRequest,
    rag_engine: RAGEngine = Depends(get_rag_engine)
) -> ChatResponse:
    """
    Process a chat query using RAG
    """
    try:
        # Extract question from either 'message' or 'question' field
        question = request.message or request.question
        if not question:
            raise HTTPException(status_code=400, detail="No question provided")
            
        logger.info(f"Processing query: {question[:50]}...")
        
        # Use conversation_id or session_id
        session_id = request.conversation_id or request.session_id
        
        # Process the query
        result = await rag_engine.query(
            question=question,
            user_id=request.user_id,
            paper_id=request.paper_id,
            session_id=session_id,
            history=request.history
        )
        
        # Format response to match frontend expectations
        response = ChatResponse(
            message=result["answer"],  # Frontend expects 'message'
            answer=result["answer"],   # Keep 'answer' for compatibility
            sources=result["sources"],
            question=question,
            conversation_id=session_id
        )
        
        return response
        
    except Exception as e:
        logger.error(f"Error processing chat query: {str(e)}")
        raise HTTPException(status_code=500, detail=str(e))

@router.post("/clear")
async def clear_conversation(
    request: ConversationClearRequest,
    rag_engine: RAGEngine = Depends(get_rag_engine)
):
    """
    Clear conversation history
    """
    try:
        session_id = request.conversation_id or request.session_id
        await rag_engine.clear_conversation(session_id=session_id)
        return {"message": "Conversation cleared successfully"}
        
    except Exception as e:
        logger.error(f"Error clearing conversation: {str(e)}")
        raise HTTPException(status_code=500, detail=str(e))

@router.get("/history")
async def get_chat_history(
    user_id: str,
    limit: int = 10,
    rag_engine: RAGEngine = Depends(get_rag_engine)
):
    """
    Get chat history for a user
    """
    try:
        # This would integrate with Java backend to fetch history
        # For now, return a placeholder
        return {
            "user_id": user_id,
            "history": [],
            "message": "Chat history endpoint - to be implemented with Java backend integration"
        }
        
    except Exception as e:
        logger.error(f"Error fetching chat history: {str(e)}")
        raise HTTPException(status_code=500, detail=str(e)) 