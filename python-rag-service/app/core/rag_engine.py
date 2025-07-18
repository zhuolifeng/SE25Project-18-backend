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
import re
import threading
from concurrent.futures import ThreadPoolExecutor, as_completed
import traceback

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
        self.section_aware_splitter = None
        self.qa_chain = None
        self.memory = None
        self.executor = ThreadPoolExecutor(max_workers=3)  # 用于并行处理
        self._lock = threading.Lock()  # 添加线程锁
        
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
                chunk_size=800,  # 减小块大小以提高精度
                chunk_overlap=150,  # 适当的重叠
                length_function=len,
                # 添加更多分隔符，按重要性排序
                separators=["\n## ", "\n\n", "\n", ". ", " ", ""]
            )
            
            # 添加一个特殊的分块器用于摘要和介绍部分
            self.section_aware_splitter = RecursiveCharacterTextSplitter(
                chunk_size=500,  # 更小的块大小，适合重要部分
                chunk_overlap=100,
                length_function=len,
                separators=["\n## ", "\n\n", "\n", ". ", " ", ""]
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
        """并行处理多个论文"""
        try:
            results = []
            errors = []
            
            # 使用线程池并行处理
            future_to_id = {
                self.executor.submit(self._process_single_paper, paper_id): paper_id
                for paper_id in paper_ids
            }
            
            for future in as_completed(future_to_id.keys()):
                paper_id = future_to_id[future]
                try:
                    result = future.result()
                    if result:
                        results.append(result)
                    else:
                        errors.append({"paper_id": paper_id, "error": "处理失败"})
                except Exception as e:
                    errors.append({"paper_id": paper_id, "error": str(e)})
                    
            return {
                "processed": len(results),
                "total": len(paper_ids),
                "errors": errors
            }
            
        except Exception as e:
            logging.error(f"Failed to process papers: {str(e)}")
            raise
    
    async def _process_single_paper(self, paper_id: str) -> Dict[str, Any]:
        """处理单个论文文档"""
        try:
            # 获取论文数据
            paper = await self.java_client.get_paper(paper_id)
            
            # 创建带结构的文档列表
            documents = self._create_paper_content(paper)
            
            # 添加到向量数据库
            doc_ids = await self.vector_store_manager.add_documents(documents)
            
            return {
                "paper_id": paper_id,
                "title": paper.get("title", ""),
                "chunks": len(documents),
                "doc_ids": doc_ids[:5]  # 仅记录前5个ID用于日志
            }
        except Exception as e:
            logging.error(f"处理论文 {paper_id} 时出错: {str(e)}")
            raise
    
    def _create_paper_content(self, paper: Dict[str, Any]) -> List[Document]:
        """创建带有结构的论文文档列表"""
        documents = []
        paper_id = str(paper.get("id", ""))
        title = paper.get("title", "")
        
        # 创建元数据基础
        base_metadata = {
            "paper_id": paper_id,
            "title": title,
            "authors": ", ".join(paper.get("authors", [])),
            "year": str(paper.get("year", "")),
            "source": paper.get("url", "")
        }
        
        # 处理摘要（高优先级）
        if paper.get("abstract"):
            abstract_text = f"# Abstract\n\n{paper['abstract']}"
            abstract_chunks = self.section_aware_splitter.split_text(abstract_text)
            
            for i, chunk in enumerate(abstract_chunks):
                documents.append(Document(
                    page_content=chunk,
                    metadata={
                        **base_metadata,
                        "section": "abstract",
                        "chunk_index": i,
                        "priority": 10  # 高优先级
                    }
                ))
        
        # 处理介绍部分（如果存在）
        if paper.get("content"):
            # 尝试提取介绍部分
            content = paper["content"]
            intro_match = re.search(r"(?i)(?:introduction|1\.(\s+)introduction).*?(?:\n#|\n2\.)", content, re.DOTALL)
            
            if intro_match:
                intro_text = intro_match.group(0)
                intro_chunks = self.section_aware_splitter.split_text(intro_text)
                
                for i, chunk in enumerate(intro_chunks):
                    documents.append(Document(
                        page_content=chunk,
                        metadata={
                            **base_metadata,
                            "section": "introduction",
                            "chunk_index": i,
                            "priority": 8  # 较高优先级
                        }
                    ))
            
            # 处理其余内容
            # 使用正则表达式分割文章主要部分
            sections = re.split(r'\n#+\s+', content)
            for i, section in enumerate(sections):
                if not section.strip():
                    continue
                    
                # 尝试提取章节标题
                section_title = section.split('\n')[0].strip()
                section_content = '\n'.join(section.split('\n')[1:])
                
                # 根据不同章节设置不同优先级
                priority = 5  # 默认优先级
                if re.search(r'(?i)conclusion|discussion|result', section_title):
                    priority = 7  # 结论和讨论部分略高优先级
                    
                section_chunks = self.text_splitter.split_text(section_content)
                for j, chunk in enumerate(section_chunks):
                    documents.append(Document(
                        page_content=chunk,
                        metadata={
                            **base_metadata,
                            "section": section_title[:50],  # 限制长度
                            "chunk_index": j,
                            "section_index": i,
                            "priority": priority
                        }
                    ))
        
        return documents
    
    async def query(self, question: str, user_id: Optional[str] = None, paper_id: Optional[str] = None, session_id: Optional[str] = None, history: Optional[List] = None) -> Dict[str, Any]:
        """改进后的查询方法"""
        if not session_id:
            session_id = str(uuid.uuid4())
            
        logging.info(f"处理查询 - 用户ID: {user_id}, 论文ID: {paper_id}, 会话ID: {session_id}")
        logging.info(f"查询问题: {question}")

        # 初始化docs变量，确保在所有路径中都有值
        docs = []
        
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
            
            # 修改检索策略
            try:
                logging.info("开始向量检索")
                start_time = time.time()
                
                # 根据问题类型调整检索量
                retrieval_k = config.RAG_TOP_K
                # 对于"什么是"、"介绍"等概述性问题增加检索量
                if re.search(r'(?i)什么是|介绍|提出了什么|核心|创新|贡献|主要|概述', question):
                    retrieval_k = config.RAG_TOP_K + 3
                    logging.info(f"概述性问题，增加检索量到 {retrieval_k}")
                
                filter_dict = {}
                if paper_id:
                    paper_id_str = str(paper_id)
                    logging.info(f"应用论文过滤器，论文ID: {paper_id_str}")
                    filter_dict = {'paper_id': paper_id_str}
                    
                    # 使用矢量搜索
                    docs = await self.vector_store_manager.search_documents(
                        query=question, 
                        k=retrieval_k,
                        filter_dict=filter_dict
                    )
                    
                    # 增加明确的标题和摘要匹配的优先级
                    if docs:
                        # 首先按元数据中的优先级进行主排序
                        docs = sorted(docs, key=lambda x: x.metadata.get("priority", 0), reverse=True)
                        
                        # 其次，对于关于核心创新、方法的问题，优先考虑摘要和介绍
                        if re.search(r'(?i)核心|创新|提出|方法|贡献|主要', question):
                            # 提升包含摘要或介绍的文档排序
                            docs = sorted(docs, 
                                         key=lambda x: 1 if re.search(r'摘要|介绍|abstract|introduction', 
                                                                    x.page_content.lower()) else 0, 
                                         reverse=True) + docs
                    
                    # 如果没有找到结果，尝试其他过滤方式
                    if not docs:
                        logging.warning(f"使用主过滤器没有找到文档，尝试备用字段")
                        # 尝试其他可能的字段名
                        alt_filters = [
                            {"metadata.paper_id": paper_id_str},
                            {"paper_id": paper_id_str},  # 再次尝试不同格式
                            {"paperId": paper_id_str},
                            {"id": paper_id_str}
                        ]
                        
                        for alt_filter in alt_filters:
                            logging.debug(f"尝试备用过滤器: {alt_filter}")
                            alt_docs = await self.vector_store_manager.search_documents(
                                query=question,
                                k=retrieval_k,
                                filter_dict=alt_filter
                            )
                            if alt_docs:
                                docs = alt_docs
                                logging.info(f"使用备用过滤器找到 {len(docs)} 个文档")
                                break
                else:
                    # 当没有指定paper_id时，也应进行文档检索
                    docs = await self.vector_store_manager.search_documents(
                        query=question, 
                        k=retrieval_k
                    )
                
                # 根据文档元数据优先级排序
                if docs:
                    # 按照元数据中的优先级和相关性重新排序
                    docs = sorted(docs, key=lambda x: x.metadata.get("priority", 5), reverse=True)
                    
                # 记录检索到的文档的元数据以便调试
                if docs:
                    logging.debug("检索到的文档元数据示例:")
                    for i, doc in enumerate(docs[:2]):
                        logging.debug(f"文档 {i}: {doc.metadata}")
                else:
                    logging.warning("没有找到相关文档！")

                retrieval_time = time.time() - start_time
                logging.info(f"向量检索完成，耗时: {retrieval_time:.2f}秒，找到文档数: {len(docs)}")
                    
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
                # 捕获内部异常并记录日志
                logging.error(f"向量检索或LLM处理过程中出错: {str(e)}")
                # 设置一个默认的空文档列表，确保代码可以继续运行
                docs = []
                # 返回一个友好的错误信息
                return {
                    "message": f"抱歉，我在处理您的问题时遇到了一些困难: {str(e)}",
                    "answer": f"处理查询时出现错误: {str(e)}",
                    "sources": [],
                    "question": question,
                    "conversation_id": session_id
                }
            
        except Exception as e:
            # 捕获最外层的异常
            logging.error(f"查询处理时出错: {str(e)}")
            traceback.print_exc()
            # 确保返回一个有效的响应
            return {
                "message": f"抱歉，处理您的问题时发生了错误: {str(e)}",
                "answer": f"处理查询时出现错误: {str(e)}",
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