"""
Documents API endpoints
"""

from fastapi import APIRouter, HTTPException, Depends
from pydantic import BaseModel
from typing import List, Dict, Any
import logging

from app.api.chat import get_rag_engine
from app.core.rag_engine import RAGEngine

logger = logging.getLogger(__name__)

router = APIRouter()

class ProcessDocumentsRequest(BaseModel):
    """Request to process documents"""
    paper_ids: List[str]

class ProcessDocumentsResponse(BaseModel):
    """Response for document processing"""
    processed: int
    total: int
    errors: List[Dict[str, Any]]
    message: str

@router.post("/process", response_model=ProcessDocumentsResponse)
async def process_documents(
    request: ProcessDocumentsRequest,
    rag_engine: RAGEngine = Depends(get_rag_engine)
) -> ProcessDocumentsResponse:
    """
    Process papers and add them to the vector database
    """
    try:
        logger.info(f"Processing {len(request.paper_ids)} papers...")
        
        result = await rag_engine.process_papers(request.paper_ids)
        
        response = ProcessDocumentsResponse(
            processed=result["processed"],
            total=result["total"],
            errors=result["errors"],
            message=f"Successfully processed {result['processed']} out of {result['total']} papers"
        )
        
        return response
        
    except Exception as e:
        logger.error(f"Error processing documents: {str(e)}")
        raise HTTPException(status_code=500, detail=str(e))

@router.get("/status")
async def get_document_status():
    """
    Get document processing status
    """
    return {
        "message": "Document processing service is ready",
        "supported_formats": ["academic_papers"],
        "vector_database": "ready"
    } 