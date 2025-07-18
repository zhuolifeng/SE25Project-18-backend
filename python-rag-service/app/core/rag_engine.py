"""
RAG Engine - Core logic for document retrieval and generation
"""

from typing import List, Dict, Any, Optional
import asyncio
from langchain.text_splitter import RecursiveCharacterTextSplitter
from langchain_community.embeddings import HuggingFaceEmbeddings
from langchain.schema import Document
from langchain.chains import ConversationalRetrievalChain
from langchain.memory import ConversationBufferMemory
import logging
import uuid
import time

from app.core.vector_store import VectorStoreManager
from app.core.llm_provider import LLMProvider
from app.integrations.java_backend import JavaBackendClient
from config import config

logger = logging.getLogger(__name__)

class RAGEngine:
    """Main RAG engine for handling document processing and queries"""
    
    def __init__(self):
        self.vector_store_manager = None
        self.llm_provider = None
        self.java_client = None
        self.embeddings = None
        self.text_splitter = None
        self.qa_chain = None
        self.memory = None
        
    async def initialize(self):
        """Initialize all components"""
        errors = []
        
        # 初始化为None，避免未定义错误
        self.embeddings = None
        self.vector_store_manager = None
        self.llm_provider = None
        self.java_client = None
        self.memory = None
        self.qa_chain = None
        
        try:
            # Initialize embeddings
            logger.info(f"Loading embedding model: {config.EMBEDDING_MODEL}")
            try:
                self.embeddings = HuggingFaceEmbeddings(
                    model_name=config.EMBEDDING_MODEL,
                    model_kwargs={'device': 'cpu'},
                    encode_kwargs={'normalize_embeddings': True}
                )
                logger.info(f"嵌入模型加载成功: {config.EMBEDDING_MODEL}")
            except Exception as e:
                error_msg = f"嵌入模型加载失败: {str(e)}"
                logger.error(error_msg)
                errors.append(error_msg)
                raise
            
            # Initialize text splitter
            self.text_splitter = RecursiveCharacterTextSplitter(
                chunk_size=config.RAG_CHUNK_SIZE,
                chunk_overlap=config.RAG_CHUNK_OVERLAP,
                length_function=len,
                separators=["\n\n", "\n", ".", " ", ""]
            )
            
            # Initialize vector store
            try:
                self.vector_store_manager = VectorStoreManager(self.embeddings)
                await self.vector_store_manager.initialize()
                logger.info("向量存储初始化成功")
            except Exception as e:
                error_msg = f"向量存储初始化失败: {str(e)}"
                logger.error(error_msg)
                errors.append(error_msg)
                raise
            
            # Initialize LLM provider
            try:
                self.llm_provider = LLMProvider()
                logger.info("LLM提供者初始化成功")
            except Exception as e:
                error_msg = f"LLM提供者初始化失败: {str(e)}"
                logger.error(error_msg)
                errors.append(error_msg)
                raise
            
            # Initialize Java backend client
            try:
                self.java_client = JavaBackendClient(config.JAVA_BACKEND_URL)
                logger.info(f"Java后端客户端初始化成功: {config.JAVA_BACKEND_URL}")
            except Exception as e:
                error_msg = f"Java后端客户端初始化失败: {str(e)}"
                logger.warning(error_msg)
                errors.append(error_msg)
                # 不抛出异常，后端客户端非关键组件
            
            # Initialize conversation memory
            self.memory = ConversationBufferMemory(
                memory_key="chat_history",
                return_messages=True,
                output_key="answer",
                input_key="question"  # 指定输入键为question
            )
            
            # Create QA chain
            try:
                self._create_qa_chain()
                logger.info("QA链创建成功")
            except Exception as e:
                error_msg = f"QA链创建失败: {str(e)}"
                logger.error(error_msg)
                errors.append(error_msg)
                raise
            
            if errors:
                logger.warning(f"RAG引擎初始化完成，但有{len(errors)}个警告")
            else:
                logger.info("RAG引擎初始化完全成功")
            
        except Exception as e:
            logger.error(f"RAG引擎初始化严重失败: {str(e)}")
            # 收集所有错误信息
            if not errors:
                errors.append(str(e))
            raise Exception(f"RAG引擎初始化失败: {'; '.join(errors)}")
            
    def _create_qa_chain(self):
        """Create the conversational retrieval chain"""
        if self.llm_provider is None or self.vector_store_manager is None:
            raise ValueError("LLM提供者或向量存储管理器未初始化")
            
        self.qa_chain = ConversationalRetrievalChain.from_llm(
            llm=self.llm_provider.get_llm(),
            retriever=self.vector_store_manager.get_retriever(k=config.RAG_TOP_K),
            memory=self.memory,
            return_source_documents=True,
            verbose=config.DEBUG_MODE
        )
        return self.qa_chain  # 添加此行返回qa_chain
    
    async def process_papers(self, paper_ids: List[str]) -> Dict[str, Any]:
        """Process papers from Java backend and add to vector store"""
        try:
            processed_count = 0
            errors = []
            
            for paper_id in paper_ids:
                try:
                    # Fetch paper from Java backend
                    paper = await self.java_client.get_paper(paper_id)
                    
                    # Create document content
                    content = self._create_paper_content(paper)
                    
                    # Split into chunks
                    chunks = self.text_splitter.split_text(content)
                    
                    # Create documents with metadata
                    documents = [
                        Document(
                            page_content=chunk,
                            metadata={
                                "paper_id": paper_id,
                                "title": paper.get("title", ""),
                                "authors": ", ".join(paper.get("authors", [])),
                                "year": paper.get("year", ""),
                                "chunk_index": i
                            }
                        )
                        for i, chunk in enumerate(chunks)
                    ]
                    
                    # Add to vector store
                    await self.vector_store_manager.add_documents(documents)
                    processed_count += 1
                    
                except Exception as e:
                    logger.error(f"Error processing paper {paper_id}: {str(e)}")
                    errors.append({"paper_id": paper_id, "error": str(e)})
            
            return {
                "processed": processed_count,
                "total": len(paper_ids),
                "errors": errors
            }
            
        except Exception as e:
            logger.error(f"Failed to process papers: {str(e)}")
            raise
    
    def _create_paper_content(self, paper: Dict[str, Any]) -> str:
        """Create searchable content from paper data"""
        parts = []
        
        if paper.get("title"):
            parts.append(f"Title: {paper['title']}")
            
        if paper.get("authors"):
            parts.append(f"Authors: {', '.join(paper['authors'])}")
            
        if paper.get("abstract"):
            parts.append(f"Abstract: {paper['abstract']}")
            
        if paper.get("content"):
            parts.append(f"Content: {paper['content']}")
            
        if paper.get("keywords"):
            parts.append(f"Keywords: {', '.join(paper['keywords'])}")
            
        return "\n\n".join(parts)
    
    async def query(self, question: str, user_id: Optional[str] = None, paper_id: Optional[str] = None, session_id: Optional[str] = None, history: Optional[List] = None) -> Dict[str, Any]:
        """
        Query the RAG system with a question
        """
        if not session_id:
            session_id = str(uuid.uuid4())
            
        logging.info(f"处理查询 - 用户ID: {user_id}, 论文ID: {paper_id}, 会话ID: {session_id}")
        logging.info(f"查询问题: {question}")

        try:
            # 构建过滤器来限定特定论文
            filter_dict = {}
            if paper_id:
                # 确保paper_id是字符串类型
                paper_id_str = str(paper_id)
                logging.info(f"应用论文过滤器，论文ID: {paper_id_str}")
                filter_dict = {'paper_id': paper_id_str}
                # 添加备用过滤键以应对可能的不一致命名
                filter_dict_alt = {"metadata.paper_id": paper_id_str}
                logging.debug(f"应用过滤器: {filter_dict}")

            # 获取相关文档
            start_time = time.time()
            logging.info("开始向量检索")
            
            if paper_id:
                # 尝试使用主过滤器
                docs = await self.vector_store_manager.search_documents(
                    query=question, 
                    k=config.RAG_TOP_K,
                    filter_dict=filter_dict
                )
                
                # 如果没有找到结果，尝试备用过滤器
                if not docs and "metadata.paper_id" in filter_dict:
                    logging.warning(f"使用主过滤器没有找到文档，尝试备用字段")
                    # 尝试其他可能的字段名
                    alt_filters = [
                        {"metadata.paperId": paper_id_str},
                        {"metadata.paper_title": {"$eq": paper_id_str}},
                        {"metadata.paperID": paper_id_str}
                    ]
                    
                    for alt_filter in alt_filters:
                        logging.debug(f"尝试备用过滤器: {alt_filter}")
                        docs = await self.vector_store_manager.search_documents(
                            query=question,
                            k=config.RAG_TOP_K,
                            filter_dict=alt_filter
                        )
                        if docs:
                            logging.info(f"使用备用过滤器找到 {len(docs)} 个文档")
                            break
            else:
                # 无过滤器的检索
                docs = await self.vector_store_manager.search_documents(
                    query=question, 
                    k=config.RAG_TOP_K
                )
            
            retrieval_time = time.time() - start_time
            logging.info(f"向量检索完成，耗时: {retrieval_time:.2f}秒，找到文档数: {len(docs)}")
            
            # 记录检索到的文档的元数据以便调试
            if docs:
                logging.debug("检索到的文档元数据示例:")
                for i, doc in enumerate(docs[:2]):
                    logging.debug(f"文档 {i}: {doc.metadata}")
            else:
                logging.warning("没有找到相关文档！")
                
            # 使用LLM生成回答
            sources = []
            for doc in docs:
                metadata = doc.metadata
                source_info = {
                    "content": doc.page_content[:200] + "...",  # 截取前200字符
                    "paper_id": metadata.get("paper_id") or metadata.get("paperId"),
                    "title": metadata.get("title") or metadata.get("paper_title"),
                    "source": metadata.get("source", ""),
                }
                sources.append(source_info)
            
            # 准备历史对话记录
            processed_history = []
            if history:
                logging.debug(f"处理历史对话，长度: {len(history)}")
                for item in history:
                    if isinstance(item, dict):
                        role = item.get("role", "")
                        content = item.get("content", "")
                        processed_history.append({"role": role, "content": content})
            
            # 执行LLM查询
            logging.info("开始LLM查询生成")
            start_time = time.time()
            # 不要重新创建，直接使用已经初始化的qa_chain
            # qa_chain = self._create_qa_chain()
            chain_result = await self.qa_chain.ainvoke({
                "question": question,
                "context": [doc.page_content for doc in docs],
                "history": processed_history
            })
            llm_time = time.time() - start_time
            logging.info(f"LLM生成完成，耗时: {llm_time:.2f}秒")
            
            # 处理结果
            answer = chain_result.get("answer", "")
            logging.info(f"生成的回答长度: {len(answer)} 字符")
            
            return {
                "message": answer,  # 保持兼容性
                "answer": answer,
                "sources": sources,
                "question": question,
                "conversation_id": session_id
            }
            
        except Exception as e:
            logging.exception(f"查询处理时出错: {str(e)}")
            return {
                "message": f"处理查询时发生错误: {str(e)}",
                "answer": f"抱歉，处理您的问题时出现了技术问题。",
                "sources": [],
                "question": question,
                "conversation_id": session_id
            }
    
    async def clear_conversation(self, session_id: Optional[str] = None):
        """Clear conversation memory"""
        self.memory.clear()
        
    async def cleanup(self):
        """Cleanup resources"""
        if self.vector_store_manager:
            await self.vector_store_manager.cleanup() 