"""
Documents API endpoints
"""

from fastapi import APIRouter, HTTPException, Depends
from pydantic import BaseModel
from typing import List, Dict, Any
import logging

from app.api.chat import get_rag_engine
from app.core.rag_engine import RAGEngine
from app.services.pdf_extractor import get_pdf_extractor

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

class ExtractPdfRequest(BaseModel):
    """Request to extract PDF content"""
    pdf_url: str
    paper_id: int
    paper_title: str

class ExtractPdfResponse(BaseModel):
    """Response for PDF extraction"""
    success: bool
    message: str
    paper_id: int
    chunks_created: int
    status: str

class PdfStatusResponse(BaseModel):
    """Response for PDF extraction status"""
    extracted: bool
    chunks_count: int
    status: str
    error: str = None

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

@router.post("/extract-pdf", response_model=ExtractPdfResponse)
async def extract_pdf_content(
    request: ExtractPdfRequest,
    rag_engine: RAGEngine = Depends(get_rag_engine)
) -> ExtractPdfResponse:
    """
    Extract PDF content and store to vector database
    """
    try:
        logger.info(f"Extracting PDF content for paper {request.paper_id}: {request.paper_title}")
        
        # 获取PDF提取器
        pdf_extractor = get_pdf_extractor(rag_engine.vector_store)
        
        # 提取PDF内容
        result = await pdf_extractor.extract_pdf_content(
            request.pdf_url,
            request.paper_id,
            request.paper_title
        )
        
        response = ExtractPdfResponse(
            success=result["success"],
            message=result["message"],
            paper_id=result["paper_id"],
            chunks_created=result["chunks_created"],
            status=result["status"]
        )
        
        return response
        
    except Exception as e:
        logger.error(f"Error extracting PDF content: {str(e)}")
        return ExtractPdfResponse(
            success=False,
            message=f"PDF extraction failed: {str(e)}",
            paper_id=request.paper_id,
            chunks_created=0,
            status="error"
        )

@router.get("/pdf-status/{paper_id}", response_model=PdfStatusResponse)
async def get_pdf_extraction_status(
    paper_id: int,
    rag_engine: RAGEngine = Depends(get_rag_engine)
) -> PdfStatusResponse:
    """
    Get PDF extraction status for a specific paper
    """
    try:
        logger.info(f"Getting PDF extraction status for paper {paper_id}")
        
        # 获取PDF提取器
        pdf_extractor = get_pdf_extractor(rag_engine.vector_store)
        
        # 获取提取状态
        result = await pdf_extractor.get_pdf_extraction_status(paper_id)
        
        response = PdfStatusResponse(
            extracted=result["extracted"],
            chunks_count=result["chunks_count"],
            status=result["status"],
            error=result.get("error")
        )
        
        return response
        
    except Exception as e:
        logger.error(f"Error getting PDF extraction status: {str(e)}")
        return PdfStatusResponse(
            extracted=False,
            chunks_count=0,
            status="error",
            error=str(e)
        ) 