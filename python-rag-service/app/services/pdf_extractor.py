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
import textwrap # Added for text wrapping

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
        """从PDF中提取文本，尝试多种方法以获得最佳结果"""
        logging.info("开始从PDF提取文本...")
        text = ""
        
        # 首先尝试PyPDF2 (处理正常PDF)
        try:
            pdf_text = self._extract_with_pypdf2(pdf_content)
            if pdf_text and len(pdf_text.strip()) > 500:  # 检查提取的文本是否足够长
                text = pdf_text
                logging.info("使用PyPDF2成功提取文本")
            else:
                logging.warning("PyPDF2提取的文本不足，尝试其他方法")
        except Exception as e:
            logging.warning(f"PyPDF2提取失败: {str(e)}")
        
        # 如果PyPDF2失败，尝试pdfplumber (更好处理复杂布局)
        if not text or len(text.strip()) < 500:
            try:
                plumber_text = self._extract_with_pdfplumber(pdf_content)
                if plumber_text and len(plumber_text.strip()) > 500:
                    text = plumber_text
                    logging.info("使用pdfplumber成功提取文本")
                else:
                    logging.warning("pdfplumber提取的文本不足")
            except Exception as e:
                logging.warning(f"pdfplumber提取失败: {str(e)}")
        
        # 如果两种方法都提取到了一些内容，选择长度更长的结果
        if not text or len(text.strip()) < 200:
            logging.error("所有PDF文本提取方法都失败")
            return ""
        
        logging.info(f"PDF文本提取完成，共{len(text)}字符")
        return text
    
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
        if text is None:
            return ""
        
        # 基础清理
        text = re.sub(r'\s+', ' ', text)
        text = re.sub(r'-\s+', '', text)
        
        # 移除重复的换行符
        text = re.sub(r'\n\s*\n', '\n\n', text)
        
        # 修复乱码
        text = re.sub(r'>SOE<|>dap<|>eod<|>SOE|SOE<|>dap|dap<', ' ', text)
        
        # 修复连在一起的单词 (使用驼峰拆分)
        text = re.sub(r'([a-z])([A-Z])', r'\1 \2', text)
        
        # 修复常见组合词
        common_words = ['the', 'and', 'for', 'that', 'with', 'this', 'from', 'is', 'to', 'in', 'of', 'on']
        for word in common_words:
            pattern = f'([a-z])({word})([A-Z]|[a-z])'
            text = re.sub(pattern, r'\1 \2 \3', text, flags=re.IGNORECASE)
        
        # 移除多余空格
        text = re.sub(r'\s+', ' ', text)
        
        # 标记重要章节
        return self._mark_important_sections(text)

    def _mark_important_sections(self, text: str) -> str:
        """识别和标记摘要和介绍部分"""
        # 标记Abstract部分
        abstract_pattern = re.compile(r'(abstract|摘要)[:\.\s\n]*(.*?)(?=\n\s*(?:introduction|引言|1\.\s*introduction|1\.\s*引言|\d+\.\s*|$))', re.IGNORECASE | re.DOTALL)
        abstract_match = abstract_pattern.search(text)
        if abstract_match:
            abstract_text = abstract_match.group(2).strip()
            if len(abstract_text) > 50:  # 确保找到的摘要有足够长度
                text = text.replace(abstract_text, f"[ABSTRACT] {abstract_text} [/ABSTRACT]")
        
        # 标记Introduction部分
        intro_pattern = re.compile(r'(introduction|引言|1\.\s*introduction|1\.\s*引言)[:\.\s\n]*(.*?)(?=\n\s*(?:\d+\.\s*|conclusion|references|参考文献|$))', re.IGNORECASE | re.DOTALL)
        intro_match = intro_pattern.search(text)
        if intro_match:
            intro_text = intro_match.group(2).strip()
            if len(intro_text) > 100:  # 确保找到的介绍有足够长度
                text = text.replace(intro_text, f"[INTRODUCTION] {intro_text} [/INTRODUCTION]")
        
        return text

    def _split_text_into_chunks(self, text: str) -> List[str]:
        """将文本分割成多个块，优先处理标记的重要部分"""
        logging.info("开始将文本分割成块...")
        
        chunks = []
        
        # 首先提取并处理摘要部分
        abstract_match = re.search(r'\[ABSTRACT\](.*?)\[\/ABSTRACT\]', text, re.DOTALL)
        if abstract_match:
            abstract_text = abstract_match.group(1).strip()
            # 将摘要分成较小的块
            abstract_chunks = textwrap.wrap(abstract_text, width=1000, replace_whitespace=False, break_long_words=False)
            for i, chunk in enumerate(abstract_chunks):
                chunks.append(f"[摘要部分 {i+1}/{len(abstract_chunks)}] {chunk}")
            
            # 从原文移除已处理的摘要
            text = text.replace(f"[ABSTRACT]{abstract_text}[/ABSTRACT]", "")
        
        # 然后提取并处理介绍部分
        intro_match = re.search(r'\[INTRODUCTION\](.*?)\[\/INTRODUCTION\]', text, re.DOTALL)
        if intro_match:
            intro_text = intro_match.group(1).strip()
            # 将介绍分成较小的块
            intro_chunks = textwrap.wrap(intro_text, width=1000, replace_whitespace=False, break_long_words=False)
            for i, chunk in enumerate(intro_chunks):
                chunks.append(f"[介绍部分 {i+1}/{len(intro_chunks)}] {chunk}")
            
            # 从原文移除已处理的介绍
            text = text.replace(f"[INTRODUCTION]{intro_text}[/INTRODUCTION]", "")
        
        # 检测并处理其他章节
        sections = re.split(r'\n\s*\d+\.', text)
        for i, section in enumerate(sections):
            if i > 0:  # 第一个通常是文章前导，不加入序号
                section = f"{i}. {section}"
            
            if len(section.strip()) > 100:  # 忽略太短的部分
                # 将章节分成较小的块
                section_chunks = textwrap.wrap(section.strip(), width=1000, replace_whitespace=False, break_long_words=False)
                for j, chunk in enumerate(section_chunks):
                    section_name = f"第{i}章" if i > 0 else "前言"
                    chunks.append(f"[{section_name} {j+1}/{len(section_chunks)}] {chunk}")
        
        logging.info(f"文本分割完成，共生成{len(chunks)}个块")
        return chunks

    async def _store_chunks_to_vector_db(self, chunks: List[str], paper_id: int, 
                                   paper_title: str, pdf_url: str) -> int:
        """将文本块存储到向量数据库"""
        logging.info(f"开始将{len(chunks)}个文本块存储到向量数据库...")
        
        documents = []
        
        # 为每个块创建Document对象
        for i, chunk in enumerate(chunks):
            # 检测块的优先级（摘要和介绍有更高优先级）
            priority = 0  # 默认优先级
            if chunk.startswith("[摘要"):
                priority = 10  # 摘要优先级最高
            elif chunk.startswith("[介绍"):
                priority = 8  # 介绍部分优先级次之
            
            metadata = {
                "paper_id": str(paper_id),
                "title": paper_title,
                "source": pdf_url,
                "chunk_id": i,
                "priority": priority,
                "timestamp": datetime.now().isoformat()
            }
            
            doc = Document(page_content=chunk, metadata=metadata)
            documents.append(doc)
        
        # 添加到向量数据库
        try:
            await self.vector_store.add_documents(documents)
            logging.info(f"成功存储 {len(documents)} 个块到向量数据库，论文ID: {paper_id}")
            return len(documents)
        except Exception as e:
            logging.error(f"向量数据库存储失败: {str(e)}")
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