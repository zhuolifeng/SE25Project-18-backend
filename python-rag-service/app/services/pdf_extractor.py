"""
PDF文本提取服务
用于从PDF文件中提取文本内容，并将其向量化存储到Qdrant数据库中
"""

import io
import re
import logging
import requests
from typing import Dict, List, Optional, Tuple
from urllib.parse import urlparse
import asyncio
import aiohttp

try:
    import PyPDF2
except ImportError:
    PyPDF2 = None

try:
    import pdfplumber
except ImportError:
    pdfplumber = None

from langchain.text_splitter import RecursiveCharacterTextSplitter
from ..core.vector_store import VectorStore
from ..models.models import PaperResponse

logger = logging.getLogger(__name__)


class PDFExtractor:
    """PDF文本提取器"""
    
    def __init__(self, vector_store: VectorStore):
        self.vector_store = vector_store
        self.text_splitter = RecursiveCharacterTextSplitter(
            chunk_size=1000,
            chunk_overlap=200,
            length_function=len,
            separators=["\n\n", "\n", ".", "!", "?", ",", " ", ""]
        )
        self.max_file_size = 50 * 1024 * 1024  # 50MB限制
        
    async def extract_pdf_content(self, pdf_url: str, paper_id: int, paper_title: str) -> Dict:
        """
        从PDF URL提取内容并存储到向量数据库
        
        Args:
            pdf_url: PDF文件URL
            paper_id: 论文ID
            paper_title: 论文标题
            
        Returns:
            提取结果字典
        """
        try:
            logger.info(f"开始提取PDF内容: {paper_title} - {pdf_url}")
            
            # 检查是否已经提取过
            if await self._is_pdf_already_extracted(paper_id):
                logger.info(f"PDF内容已存在，跳过提取: {paper_title}")
                return {
                    "success": True,
                    "message": "PDF内容已存在",
                    "paper_id": paper_id,
                    "chunks_created": 0,
                    "status": "already_exists"
                }
            
            # 下载PDF文件
            pdf_content = await self._download_pdf(pdf_url)
            if not pdf_content:
                raise Exception("PDF下载失败")
            
            # 提取文本内容
            extracted_text = self._extract_text_from_pdf(pdf_content)
            if not extracted_text.strip():
                raise Exception("PDF文本提取失败或为空")
            
            # 清理和格式化文本
            cleaned_text = self._clean_text(extracted_text)
            
            # 分块处理
            chunks = self._split_text_into_chunks(cleaned_text)
            
            # 存储到向量数据库
            chunks_created = await self._store_chunks_to_vector_db(
                chunks, paper_id, paper_title, pdf_url
            )
            
            logger.info(f"PDF内容提取成功: {paper_title} - 创建了{chunks_created}个文本块")
            
            return {
                "success": True,
                "message": "PDF内容提取成功",
                "paper_id": paper_id,
                "chunks_created": chunks_created,
                "text_length": len(cleaned_text),
                "status": "success"
            }
            
        except Exception as e:
            logger.error(f"PDF内容提取失败: {paper_title} - {str(e)}")
            return {
                "success": False,
                "message": f"PDF内容提取失败: {str(e)}",
                "paper_id": paper_id,
                "chunks_created": 0,
                "status": "failed"
            }
    
    async def _is_pdf_already_extracted(self, paper_id: int) -> bool:
        """检查PDF是否已经提取过内容"""
        try:
            # 在Qdrant中搜索是否存在该论文的PDF内容
            search_results = await self.vector_store.search_documents(
                query_text=f"paper_id_{paper_id}",
                top_k=1,
                score_threshold=0.1
            )
            
            # 检查是否存在PDF内容标记
            for result in search_results:
                metadata = result.get("metadata", {})
                if (metadata.get("paper_id") == paper_id and 
                    metadata.get("content_type") == "pdf_content"):
                    return True
            
            return False
            
        except Exception as e:
            logger.error(f"检查PDF提取状态失败: {str(e)}")
            return False
    
    async def _download_pdf(self, pdf_url: str) -> Optional[bytes]:
        """下载PDF文件"""
        try:
            timeout = aiohttp.ClientTimeout(total=30)
            async with aiohttp.ClientSession(timeout=timeout) as session:
                headers = {
                    'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36'
                }
                
                async with session.get(pdf_url, headers=headers) as response:
                    if response.status == 200:
                        content = await response.read()
                        
                        # 检查文件大小
                        if len(content) > self.max_file_size:
                            logger.warning(f"PDF文件过大: {len(content)} bytes")
                            return None
                        
                        # 检查是否是PDF格式
                        if not self._is_valid_pdf(content):
                            logger.warning("下载的文件不是有效的PDF格式")
                            return None
                        
                        return content
                    else:
                        logger.error(f"PDF下载失败: HTTP {response.status}")
                        return None
                        
        except asyncio.TimeoutError:
            logger.error("PDF下载超时")
            return None
        except Exception as e:
            logger.error(f"PDF下载异常: {str(e)}")
            return None
    
    def _is_valid_pdf(self, content: bytes) -> bool:
        """检查是否是有效的PDF文件"""
        try:
            # 检查PDF魔术头
            if content[:4] == b'%PDF':
                return True
            return False
        except:
            return False
    
    def _extract_text_from_pdf(self, pdf_content: bytes) -> str:
        """从PDF内容中提取文本"""
        extracted_text = ""
        
        # 尝试使用pdfplumber提取文本
        if pdfplumber:
            try:
                extracted_text = self._extract_with_pdfplumber(pdf_content)
                if extracted_text.strip():
                    logger.info("使用pdfplumber成功提取文本")
                    return extracted_text
            except Exception as e:
                logger.warning(f"pdfplumber提取失败: {str(e)}")
        
        # 尝试使用PyPDF2提取文本
        if PyPDF2:
            try:
                extracted_text = self._extract_with_pypdf2(pdf_content)
                if extracted_text.strip():
                    logger.info("使用PyPDF2成功提取文本")
                    return extracted_text
            except Exception as e:
                logger.warning(f"PyPDF2提取失败: {str(e)}")
        
        # 如果都失败了，返回空字符串
        logger.error("所有PDF文本提取方法都失败了")
        return ""
    
    def _extract_with_pdfplumber(self, pdf_content: bytes) -> str:
        """使用pdfplumber提取文本"""
        text_parts = []
        
        with pdfplumber.open(io.BytesIO(pdf_content)) as pdf:
            for page_num, page in enumerate(pdf.pages):
                try:
                    text = page.extract_text()
                    if text:
                        text_parts.append(f"--- Page {page_num + 1} ---\n{text}")
                except Exception as e:
                    logger.warning(f"页面{page_num + 1}文本提取失败: {str(e)}")
                    continue
        
        return "\n\n".join(text_parts)
    
    def _extract_with_pypdf2(self, pdf_content: bytes) -> str:
        """使用PyPDF2提取文本"""
        text_parts = []
        
        pdf_reader = PyPDF2.PdfReader(io.BytesIO(pdf_content))
        
        for page_num, page in enumerate(pdf_reader.pages):
            try:
                text = page.extract_text()
                if text:
                    text_parts.append(f"--- Page {page_num + 1} ---\n{text}")
            except Exception as e:
                logger.warning(f"页面{page_num + 1}文本提取失败: {str(e)}")
                continue
        
        return "\n\n".join(text_parts)
    
    def _clean_text(self, text: str) -> str:
        """清理和格式化文本"""
        # 移除过多的空白字符
        text = re.sub(r'\s+', ' ', text)
        
        # 移除特殊字符和控制字符
        text = re.sub(r'[^\w\s\.,;:!?()[\]{}"\'`~@#$%^&*+=<>/-]', '', text)
        
        # 处理常见的PDF提取问题
        text = text.replace('ﬁ', 'fi')
        text = text.replace('ﬂ', 'fl')
        text = text.replace('ﬀ', 'ff')
        text = text.replace('ﬃ', 'ffi')
        text = text.replace('ﬄ', 'ffl')
        
        # 移除过短的行
        lines = text.split('\n')
        cleaned_lines = []
        for line in lines:
            line = line.strip()
            if len(line) > 10:  # 只保留长度超过10的行
                cleaned_lines.append(line)
        
        return '\n'.join(cleaned_lines)
    
    def _split_text_into_chunks(self, text: str) -> List[str]:
        """将文本分割成块"""
        try:
            chunks = self.text_splitter.split_text(text)
            logger.info(f"文本分割成{len(chunks)}个块")
            return chunks
        except Exception as e:
            logger.error(f"文本分割失败: {str(e)}")
            # 如果分割失败，返回整个文本作为一个块
            return [text]
    
    async def _store_chunks_to_vector_db(self, chunks: List[str], paper_id: int, 
                                       paper_title: str, pdf_url: str) -> int:
        """将文本块存储到向量数据库"""
        try:
            documents = []
            metadatas = []
            
            for i, chunk in enumerate(chunks):
                if len(chunk.strip()) < 50:  # 跳过过短的块
                    continue
                
                documents.append(chunk)
                metadatas.append({
                    "paper_id": paper_id,
                    "paper_title": paper_title,
                    "pdf_url": pdf_url,
                    "chunk_index": i,
                    "content_type": "pdf_content",
                    "source": "pdf_extractor"
                })
            
            if documents:
                # 存储到向量数据库
                await self.vector_store.add_documents(documents, metadatas)
                logger.info(f"成功存储{len(documents)}个文本块到向量数据库")
                return len(documents)
            else:
                logger.warning("没有有效的文本块可存储")
                return 0
                
        except Exception as e:
            logger.error(f"存储文本块到向量数据库失败: {str(e)}")
            return 0
    
    async def get_pdf_extraction_status(self, paper_id: int) -> Dict:
        """获取PDF提取状态"""
        try:
            # 搜索该论文的PDF内容
            search_results = await self.vector_store.search_documents(
                query_text=f"paper_id_{paper_id}",
                top_k=10,
                score_threshold=0.1
            )
            
            pdf_chunks = []
            for result in search_results:
                metadata = result.get("metadata", {})
                if (metadata.get("paper_id") == paper_id and 
                    metadata.get("content_type") == "pdf_content"):
                    pdf_chunks.append(result)
            
            if pdf_chunks:
                return {
                    "extracted": True,
                    "chunks_count": len(pdf_chunks),
                    "status": "success"
                }
            else:
                return {
                    "extracted": False,
                    "chunks_count": 0,
                    "status": "not_extracted"
                }
                
        except Exception as e:
            logger.error(f"获取PDF提取状态失败: {str(e)}")
            return {
                "extracted": False,
                "chunks_count": 0,
                "status": "error",
                "error": str(e)
            }


# 单例实例
pdf_extractor = None

def get_pdf_extractor(vector_store: VectorStore) -> PDFExtractor:
    """获取PDF提取器单例"""
    global pdf_extractor
    if pdf_extractor is None:
        pdf_extractor = PDFExtractor(vector_store)
    return pdf_extractor