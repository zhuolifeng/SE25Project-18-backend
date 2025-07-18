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
from datetime import datetime

try:
    import PyPDF2
except ImportError:
    PyPDF2 = None

try:
    import pdfplumber
except ImportError:
    pdfplumber = None

from langchain.text_splitter import RecursiveCharacterTextSplitter
from langchain.docstore.document import Document
from ..core.vector_store import VectorStoreManager
from config import config


logger = logging.getLogger(__name__)


class PDFExtractor:
    """PDF content extraction and vectorization"""

    def __init__(self, vector_store:VectorStoreManager):
        """Initialize with vector store manager"""
        self.vector_store = vector_store
        self.chunk_size = config.RAG_CHUNK_SIZE
        self.chunk_overlap = config.RAG_CHUNK_OVERLAP
        self._extraction_status = {}  # Track extraction status by paper_id
        
        # 检查PDF提取依赖库
        self.pdf_extraction_available = False
        if PyPDF2 is not None:
            self.pdf_extraction_available = True
            logging.info("PyPDF2库可用")
        if pdfplumber is not None:
            self.pdf_extraction_available = True
            logging.info("pdfplumber库可用")
        
        if not self.pdf_extraction_available:
            logging.error("警告：PyPDF2和pdfplumber都未安装，PDF提取功能不可用！")
            
        logging.info(f"Initialized PDFExtractor with chunk_size={self.chunk_size}, overlap={self.chunk_overlap}")

    async def extract_pdf_content(self, pdf_url: str, paper_id: int, paper_title: str) -> Dict:
        """
        Extract text from PDF URL and store in vector database
        """
        try:
            # 增加调试日志
            logging.info(f"开始处理PDF - URL: {pdf_url}, 论文ID: {paper_id}, 标题: {paper_title}")
            
            # Check if this paper is already extracted
            already_extracted = await self._is_pdf_already_extracted(paper_id)
            logging.debug(f"论文已提取状态检查: {already_extracted}")
            if already_extracted:
                logging.info(f"论文ID {paper_id} 已经被处理过，跳过提取")
                return {
                    "success": True,
                    "message": "PDF already processed",
                    "paper_id": paper_id,
                    "chunks_created": 0,
                    "status": "already_exists"
                }

            # Download the PDF
            logging.info(f"开始下载PDF: {pdf_url}")
            pdf_content = await self._download_pdf(pdf_url)
            if not pdf_content:
                error_msg = f"无法下载PDF: {pdf_url}"
                logging.error(error_msg)
                return {
                    "success": False,
                    "message": error_msg,
                    "paper_id": paper_id,
                    "chunks_created": 0,
                    "status": "download_failed"
                }
            
            logging.debug(f"PDF下载成功，大小: {len(pdf_content)} 字节")
            
            # Validate the PDF
            if not self._is_valid_pdf(pdf_content):
                error_msg = f"无效的PDF内容: {pdf_url}"
                logging.error(error_msg)
                return {
                    "success": False,
                    "message": error_msg,
                    "paper_id": paper_id,
                    "chunks_created": 0,
                    "status": "invalid_pdf"
                }
            
            logging.info("PDF验证通过，开始提取文本")

            # Extract text from PDF
            extracted_text = self._extract_text_from_pdf(pdf_content)
            if not extracted_text or len(extracted_text.strip()) < 50:  # 文本太短可能意味着提取失败
                error_msg = f"无法提取有效文本，或文本太少: {len(extracted_text) if extracted_text else 0} 字符"
                logging.error(error_msg)
                # 尝试其他提取方法
                logging.info("尝试使用备用方法提取...")
                extracted_text = self._extract_with_pdfplumber(pdf_content)
                if not extracted_text or len(extracted_text.strip()) < 50:
                    error_msg = "所有PDF提取方法都失败"
                    logging.error(error_msg)
                    return {
                        "success": False, 
                        "message": error_msg,
                        "paper_id": paper_id,
                        "chunks_created": 0,
                        "status": "extraction_failed"
                    }
            
            logging.info(f"成功提取文本，大小: {len(extracted_text)} 字符")
            logging.debug(f"提取的前500字符: {extracted_text[:500]}...")
            
            # Clean and split the text into chunks
            cleaned_text = self._clean_text(extracted_text)
            logging.debug(f"清理后文本大小: {len(cleaned_text)} 字符")
            
            chunks = self._split_text_into_chunks(cleaned_text)
            logging.info(f"文本已分割成 {len(chunks)} 个块")
            
            if not chunks:
                error_msg = "文本分块失败，无法创建向量存储"
                logging.error(error_msg)
                return {
                    "success": False,
                    "message": error_msg,
                    "paper_id": paper_id,
                    "chunks_created": 0,
                    "status": "chunking_failed"
                }
            
            # Store chunks in vector database
            logging.info(f"开始将 {len(chunks)} 个文本块存储到向量数据库")
            chunks_created = await self._store_chunks_to_vector_db(
                chunks, paper_id, paper_title, pdf_url)
            
            logging.info(f"成功存储 {chunks_created} 个块到向量数据库，论文ID: {paper_id}")
            
            return {
                "success": True,
                "message": f"Successfully processed PDF and created {chunks_created} chunks",
                "paper_id": paper_id,
                "chunks_created": chunks_created,
                "status": "success"
            }
            
        except Exception as e:
            error_msg = f"处理PDF时发生异常: {str(e)}"
            logging.exception(error_msg)
            import traceback
            logging.error(traceback.format_exc())
            return {
                "success": False,
                "message": error_msg,
                "paper_id": paper_id,
                "chunks_created": 0,
                "status": "exception"
            }
    
    async def _is_pdf_already_extracted(self, paper_id: int) -> bool:
        """检查PDF是否已经提取过内容"""
        try:
            # 在Qdrant中搜索是否存在该论文的PDF内容
            search_results = await self.vector_store.search_documents(
                query=f"paper_id_{paper_id}",
                k=1
            )
            
            # 检查是否存在PDF内容标记
            for result in search_results:
                metadata = {}
                try:
                    # 直接尝试获取metadata属性
                    if hasattr(result, "metadata"):
                        metadata = result.metadata
                    # 如果是字典类型
                    elif isinstance(result, dict):
                        metadata = result.get("metadata", {})
                except Exception as e:
                    logger.warning(f"获取metadata出错: {str(e)}")
                
                if (metadata.get("paper_id") == paper_id and 
                    (metadata.get("content_type") == "pdf_content" or metadata.get("chunk_type") == "pdf_content")):
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
                        if len(content) > config.MAX_PDF_SIZE: # Use config.MAX_PDF_SIZE
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
        if not self.pdf_extraction_available:
            logger.error("没有可用的PDF提取库(PyPDF2/pdfplumber)，无法提取文本")
            return ""
            
        # 尝试使用pdfplumber提取文本
        if pdfplumber:
            try:
                with pdfplumber.open(io.BytesIO(pdf_content)) as pdf:
                    pages_text = []
                    for page_num, page in enumerate(pdf.pages):
                        try:
                            text = page.extract_text()
                            if text:
                                # 特殊处理前几页（通常包含摘要和介绍）
                                if page_num < 3:
                                    pages_text.append(f"[IMPORTANT_SECTION] --- Page {page_num + 1} ---\n{text}")
                                else:
                                    pages_text.append(f"--- Page {page_num + 1} ---\n{text}")
                        except Exception as e:
                            logger.warning(f"页面{page_num + 1}文本提取失败: {str(e)}")
                            continue
                    
                    logger.info(f"使用pdfplumber成功提取了{len(pages_text)}页内容")
                    extracted_text = "\n\n".join(pages_text)
                    
                    # 识别和标记摘要和介绍部分
                    extracted_text = self._mark_important_sections(extracted_text)
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
        if pdfplumber is None:
            logger.error("pdfplumber库未安装，无法使用此提取方法")
            return ""
            
        text_parts = []
        
        try:
            with pdfplumber.open(io.BytesIO(pdf_content)) as pdf:
                for page_num, page in enumerate(pdf.pages):
                    try:
                        text = page.extract_text()
                        if text:
                            text_parts.append(f"--- Page {page_num + 1} ---\n{text}")
                    except Exception as e:
                        logger.warning(f"页面{page_num + 1}文本提取失败: {str(e)}")
                        continue
            
            logger.info(f"使用pdfplumber成功提取了{len(text_parts)}页内容")
        except Exception as e:
            logger.error(f"pdfplumber提取整体失败: {str(e)}")
            return ""
        
        return "\n\n".join(text_parts)
    
    def _extract_with_pypdf2(self, pdf_content: bytes) -> str:
        """使用PyPDF2提取文本"""
        if PyPDF2 is None:
            logger.error("PyPDF2库未安装，无法使用此提取方法")
            return ""
            
        text_parts = []
        
        try:
            pdf_reader = PyPDF2.PdfReader(io.BytesIO(pdf_content))
            
            for page_num, page in enumerate(pdf_reader.pages):
                try:
                    text = page.extract_text()
                    if text:
                        text_parts.append(f"--- Page {page_num + 1} ---\n{text}")
                except Exception as e:
                    logger.warning(f"页面{page_num + 1}文本提取失败: {str(e)}")
                    continue
            
            logger.info(f"使用PyPDF2成功提取了{len(text_parts)}页内容")
        except Exception as e:
            logger.error(f"PyPDF2提取整体失败: {str(e)}")
            return ""
        
        return "\n\n".join(text_parts)
    
    def _clean_text(self, text: str) -> str:
        """清理和格式化文本"""
        # 原有代码保留
        
        # 1. 修复乱码
        text = re.sub(r'>SOE<|>dap<|>eod<', ' ', text)
        
        # 2. 修复词语连接问题 - 使用正则表达式识别驼峰式连接词
        text = re.sub(r'([a-z])([A-Z])', r'\1 \2', text)
        
        # 3. 检测并修复常见的合并单词模式
        common_words = ['the', 'and', 'for', 'that', 'with', 'this', 'from']
        for word in common_words:
            pattern = f'([a-z])({word})([A-Z]|[a-z])'
            text = re.sub(pattern, r'\1 \2 \3', text, flags=re.IGNORECASE)
        
        # 4. 确保空格正确
        text = re.sub(r'\s+', ' ', text)
        
        # 清理后的文本
        lines = text.split('\n')
        cleaned_lines = []
        for line in lines:
            line = line.strip()
            if len(line) > 3:  # 保留更多的行（3个字符以上）
                cleaned_lines.append(line)
        
        cleaned_text = '\n'.join(cleaned_lines)
        logger.info(f"文本清理：原长度{len(text)}字符，清理后{len(cleaned_text)}字符")
        return cleaned_text
    
    def _split_text_into_chunks(self, text: str) -> List[str]:
        """将文本分割成块"""
        try:
            # 特别处理包含[IMPORTANT_SECTION]标记的文本
            important_sections = []
            regular_text = text
            
            # 提取出重要章节
            important_pattern = re.compile(r'\[IMPORTANT_SECTION\].*?(?=\[IMPORTANT_SECTION\]|$)', re.DOTALL)
            for match in important_pattern.finditer(text):
                important_section = match.group(0)
                important_sections.append(important_section)
                # 从原文中移除重要章节，避免重复
                regular_text = regular_text.replace(important_section, '')
            
            # 对重要章节使用较小的块大小进行分割
            important_chunks = []
            for section in important_sections:
                section_chunks = RecursiveCharacterTextSplitter(
                    chunk_size=500,  # 较小的块大小
                    chunk_overlap=100,
                    length_function=len,
                    separators=["\n\n", "\n", ".", "!", "?", ",", " ", ""]
                ).split_text(section)
                important_chunks.extend(section_chunks)
            
            # 对常规文本使用标准块大小进行分割
            regular_chunks = RecursiveCharacterTextSplitter(
                chunk_size=self.chunk_size,
                chunk_overlap=self.chunk_overlap,
                length_function=len,
                separators=["\n\n", "\n", ".", "!", "?", ",", " ", ""]
            ).split_text(regular_text)
            
            # 优先返回重要章节的块
            chunks = important_chunks + regular_chunks
            logger.info(f"文本分割成{len(chunks)}个块 (重要章节:{len(important_chunks)}, 常规章节:{len(regular_chunks)})")
            return chunks
            
        except Exception as e:
            logger.error(f"文本分割失败: {str(e)}")
            # 如果分割失败，返回整个文本作为一个块
            return [text]
    
    async def _store_chunks_to_vector_db(self, chunks: List[str], paper_id: int, 
                               paper_title: str, pdf_url: str) -> int:
        """Store text chunks to vector database"""
        try:
            docs = []
            for i, chunk in enumerate(chunks):
                if not chunk.strip():  # 跳过空白块
                    logging.debug(f"跳过空白文本块 {i}")
                    continue
                    
                # 设置优先级
                priority = 5  # 默认优先级
                
                # 检测重要章节
                if "[IMPORTANT_SECTION]" in chunk:
                    priority = 10
                    if "[ABSTRACT]" in chunk:
                        priority = 15  # 摘要最高优先级
                    elif "[INTRODUCTION]" in chunk:
                        priority = 12  # 介绍次高优先级
                    
                    # 移除标记，避免它们出现在实际文本中
                    chunk = chunk.replace("[IMPORTANT_SECTION]", "")
                    chunk = chunk.replace("[ABSTRACT]", "")
                    chunk = chunk.replace("[INTRODUCTION]", "")
                
                metadata = {
                    "paper_id": str(paper_id),  # 确保paper_id是字符串
                    "title": paper_title,
                    "source": pdf_url,
                    "chunk_id": i,
                    "priority": priority
                }
                
                logging.debug(f"创建块 {i}, 大小: {len(chunk)} 字符, 优先级: {priority}")
                doc = Document(page_content=chunk, metadata=metadata)
                docs.append(doc)
            
            logging.info(f"准备存储 {len(docs)} 个文档到向量数据库")
            if not docs:
                logging.warning("没有有效文档可存储")
                return 0
                
            # 记录metadata示例以便调试
            logging.debug(f"示例元数据: {docs[0].metadata if docs else 'No docs'}")
            
            # Store documents in vector database
            doc_ids = await self.vector_store.add_documents(docs)
            logging.info(f"成功添加 {len(doc_ids)} 个文档到向量数据库，返回的ID: {doc_ids[:3]}...")
            
            return len(doc_ids)
        except Exception as e:
            logging.exception(f"存储向量时出错: {str(e)}")
            return 0
    
    async def get_pdf_extraction_status(self, paper_id: int) -> Dict:
        """获取PDF提取状态"""
        try:
            # 搜索该论文的PDF内容
            search_results = await self.vector_store.search_documents(
                query=f"paper_id_{paper_id}",
                k=10
            )
            
            pdf_chunks = []
            for result in search_results:
                metadata = {}
                try:
                    # 直接尝试获取metadata属性
                    if hasattr(result, "metadata"):
                        metadata = result.metadata
                    # 如果是字典类型
                    elif isinstance(result, dict):
                        metadata = result.get("metadata", {})
                except Exception as e:
                    logger.warning(f"获取metadata出错: {str(e)}")
                
                if (metadata.get("paper_id") == paper_id and 
                    (metadata.get("content_type") == "pdf_content" or metadata.get("chunk_type") == "pdf_content")):
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

def get_pdf_extractor(vector_store: VectorStoreManager) -> PDFExtractor:
    """获取PDF提取器单例"""
    global pdf_extractor
    if pdf_extractor is None:
        pdf_extractor = PDFExtractor(vector_store)
    return pdf_extractor

def _mark_important_sections(self, text: str) -> str:
    """识别和标记摘要和介绍部分"""
    # 标记Abstract部分
    abstract_pattern = re.compile(r'(abstract|摘要)[:\.\s\n]*(.*?)(?=\n\s*(?:introduction|引言|1\.\s*introduction|1\.\s*引言|\d+\.\s*|$))', re.IGNORECASE | re.DOTALL)
    abstract_match = abstract_pattern.search(text)
    if abstract_match:
        abstract_text = abstract_match.group(2).strip()
        if len(abstract_text) > 50:  # 确保找到的摘要有足够长度
            text = text.replace(abstract_match.group(0), 
                               f"[IMPORTANT_SECTION][ABSTRACT]\n{abstract_match.group(0)}")
    
    # 标记Introduction部分
    intro_pattern = re.compile(r'(introduction|引言|1\.\s*introduction|1\.\s*引言)[:\.\s\n]*(.*?)(?=\n\s*(?:\d+\.\s*|\w+\s*\n|$))', re.IGNORECASE | re.DOTALL)
    intro_match = intro_pattern.search(text)
    if intro_match:
        intro_text = intro_match.group(2).strip()
        if len(intro_text) > 100:  # 确保找到的引言有足够长度
            text = text.replace(intro_match.group(0), 
                              f"[IMPORTANT_SECTION][INTRODUCTION]\n{intro_match.group(0)}")
    
    return text