"""
Documents API endpoints
"""

import os
import logging
import traceback

from fastapi import APIRouter, HTTPException, Depends
from pydantic import BaseModel
from typing import List, Dict, Any, Optional

from app.api.chat import get_rag_engine
from app.core.rag_engine import RAGEngine
from app.services.pdf_extractor import get_pdf_extractor

logger = logging.getLogger(__name__)

router = APIRouter()

# 尝试记录一些诊断信息
try:
    # 获取当前工作目录
    cwd = os.getcwd()
    log_file = os.path.join(cwd, "rag_service_debug.log")
    log_exists = os.path.exists(log_file)
    log_size = os.path.getsize(log_file) if log_exists else 0
    log_writable = os.access(os.path.dirname(log_file), os.W_OK) if log_exists else False
    
    # 记录诊断信息
    print(f"Documents API 初始化")
    print(f"当前工作目录: {cwd}")
    print(f"日志文件路径: {log_file}")
    print(f"日志文件存在: {log_exists}")
    print(f"日志文件大小: {log_size} 字节")
    print(f"日志目录可写: {log_writable}")
    
    # 尝试写入日志
    logging.info("========= Documents API 模块已加载 =========")
    logging.debug("Documents API 诊断信息: CWD=%s, 日志文件=%s, 存在=%s, 大小=%d, 可写=%s", 
                 cwd, log_file, log_exists, log_size, log_writable)
except Exception as e:
    print(f"Documents API 初始化诊断失败: {str(e)}")
    traceback.print_exc()

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
    debug_logs: Optional[List[Dict[str, Any]]] = None

class PdfStatusResponse(BaseModel):
    """Response for PDF extraction status"""
    extracted: bool
    chunks_count: int
    status: str
    error: Optional[str] = None  # 使用Optional类型

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
        
        # 检查请求参数
        if not request.pdf_url:
            logger.error("缺少PDF URL")
            return ExtractPdfResponse(
                success=False,
                message="请提供PDF URL",
                paper_id=request.paper_id,
                chunks_created=0,
                status="error"
            )
            
        if not request.paper_title:
            logger.warning("未提供论文标题，这可能影响搜索质量")
        
        # 获取PDF提取器 - 添加空值检查
        if rag_engine.vector_store_manager is None:
            raise ValueError("向量存储管理器未初始化")
            
        pdf_extractor = get_pdf_extractor(rag_engine.vector_store_manager)
        
        # 检查PDF提取器是否可用
        if not pdf_extractor.pdf_extraction_available:
            logger.error("PDF提取功能不可用，缺少依赖库")
            return ExtractPdfResponse(
                success=False,
                message="PDF提取功能不可用，服务器缺少PyPDF2或pdfplumber库",
                paper_id=request.paper_id,
                chunks_created=0,
                status="error"
            )
        
        # 提取PDF内容
        result = await pdf_extractor.extract_pdf_content(
            request.pdf_url,
            request.paper_id,
            request.paper_title
        )
        
        # 构建响应
        if "success" in result:
            return ExtractPdfResponse(
                success=result.get("success", False),
                message=result.get("message", ""),
                paper_id=request.paper_id,
                chunks_created=result.get("chunks_created", 0),
                status=result.get("status", "unknown")
            )
        else:
            # 兼容旧格式
            status = result.get("status", "unknown")
            chunks_count = result.get("chunks_count", 0)
            error = result.get("error", None)
            
            success = status == "success" or status == "completed"
            message = error if error else f"PDF处理状态: {status}"
            
            if status == "completed" or result.get("extracted", False):
                message = f"成功提取并存储{chunks_count}个文本块"
            
            return ExtractPdfResponse(
                success=success,
                message=message,
                paper_id=request.paper_id,
                chunks_created=chunks_count,
                status=status
            )
        
    except Exception as e:
        logger.error(f"Error extracting PDF content: {str(e)}", exc_info=True)
        return ExtractPdfResponse(
            success=False,
            message=f"PDF提取过程出错: {str(e)}",
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
    检查指定论文的PDF提取状态
    """
    logging.info(f"检查论文ID {paper_id} 的PDF提取状态")
    try:
        # 使用PDF提取器获取状态
        if rag_engine.vector_store_manager is None:
            raise ValueError("向量存储管理器未初始化")
            
        extractor = get_pdf_extractor(rag_engine.vector_store_manager)
        status = await extractor.get_pdf_extraction_status(paper_id)
        
        logging.info(f"论文 {paper_id} 的提取状态: {status}")
        
        return PdfStatusResponse(
            extracted=status.get("extracted", False),
            chunks_count=status.get("chunks_count", 0),
            status=status.get("status", "unknown"),
            error=status.get("error")
        )
    except Exception as e:
        logging.exception(f"获取PDF提取状态时出错: {str(e)}")
        return PdfStatusResponse(
            extracted=False,
            chunks_count=0,
            status="error",
            error=str(e)
        )

@router.get("/debug-vector/{paper_id}")
async def debug_vector_store_for_paper(
    paper_id: int,
    rag_engine: RAGEngine = Depends(get_rag_engine)
):
    """
    用于调试：获取向量数据库中特定论文的存储信息
    """
    logging.info(f"调试论文ID {paper_id} 在向量数据库中的存储")
    try:
        # 检查向量存储初始化状态
        if rag_engine.vector_store_manager is None:
            return {
                "error": "向量存储管理器未初始化",
                "paper_id": paper_id,
                "chunks_found": 0
            }
            
        # 构建过滤器
        filter_dict = {"metadata.paper_id": str(paper_id)}
        
        # 测试直接查询，无需搜索相似度
        docs = await rag_engine.vector_store_manager.search_documents(
            query="",  # 空查询，只用过滤器查找
            k=50,  # 获取足够的文档
            filter_dict=filter_dict
        )
        
        # 获取论文的文本块计数
        chunks_count = len(docs)
        
        # 构建响应
        result = {
            "paper_id": paper_id,
            "chunks_found": chunks_count,
            "vector_store_type": "qdrant",  # 简化，直接使用已知的向量存储类型
            "sample_chunks": []
        }
        
        # 添加样本块
        if docs:
            for i, doc in enumerate(docs[:3]):  # 只返回前3个块
                result["sample_chunks"].append({
                    "chunk_id": i,
                    "metadata": doc.metadata,
                    "content_preview": doc.page_content[:200] + "..." if len(doc.page_content) > 200 else doc.page_content
                })
        
        logging.info(f"找到论文ID {paper_id} 的 {chunks_count} 个文本块")
        return result
    
    except Exception as e:
        logging.exception(f"调试向量数据库时出错: {str(e)}")
        return {
            "error": str(e),
            "paper_id": paper_id,
            "chunks_found": 0
        }

@router.post("/extract-pdf-debug", response_model=ExtractPdfResponse)
async def extract_pdf_content_debug(
    request: ExtractPdfRequest,
    rag_engine: RAGEngine = Depends(get_rag_engine)
) -> ExtractPdfResponse:
    """
    调试版本：提取PDF内容并保存到向量数据库，返回详细过程日志
    """
    logging.info(f"开始调试PDF提取 - URL: {request.pdf_url}, 论文ID: {request.paper_id}")
    
    debug_logs = []
    
    try:
        # 配置日志捕获处理器
        class LogCaptureHandler(logging.Handler):
            def __init__(self):
                super().__init__()
                self.logs = []
                
            def emit(self, record):
                log_entry = {
                    "level": record.levelname,
                    "message": self.format(record),
                    "time": record.created
                }
                self.logs.append(log_entry)
        
        # 创建并安装日志处理器
        capture_handler = LogCaptureHandler()
        capture_handler.setFormatter(logging.Formatter('%(asctime)s - %(levelname)s - %(message)s'))
        logging.getLogger().addHandler(capture_handler)
        
        # 执行PDF提取
        if rag_engine.vector_store_manager is None:
            raise ValueError("向量存储管理器未初始化")
            
        extractor = get_pdf_extractor(rag_engine.vector_store_manager)
        result = await extractor.extract_pdf_content(
            pdf_url=request.pdf_url,
            paper_id=request.paper_id,
            paper_title=request.paper_title
        )
        
        # 获取捕获的日志
        debug_logs = capture_handler.logs
        
        # 移除日志处理器
        logging.getLogger().removeHandler(capture_handler)
        
        # 构建响应
        response = ExtractPdfResponse(
            success=result.get("success", False),
            message=result.get("message", ""),
            paper_id=request.paper_id,
            chunks_created=result.get("chunks_created", 0),
            status=result.get("status", "unknown"),
            debug_logs=debug_logs  # 添加调试日志到响应
        )
        
        return response
        
    except Exception as e:
        logging.exception(f"调试PDF提取时发生异常: {str(e)}")
        return ExtractPdfResponse(
            success=False,
            message=f"调试提取时出错: {str(e)}",
            paper_id=request.paper_id,
            chunks_created=0,
            status="exception",
            debug_logs=debug_logs
        ) 