"""
Vector Store Manager
Handles vector database operations for document storage and retrieval
"""

from typing import List, Optional, Dict, Any
import logging
import sys
import os

# 添加项目根目录到Python路径，使导入能够正确解析
root_path = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", ".."))
if root_path not in sys.path:
    sys.path.append(root_path)

from langchain_community.vectorstores import Qdrant
from langchain.schema import Document
from qdrant_client import QdrantClient
from qdrant_client.http import models

# 尝试导入配置，如果失败则使用默认值
try:
    from config import config
except (ImportError, AttributeError) as e:
    logging.warning(f"无法导入配置，使用默认值: {str(e)}")
    # 定义一个简单的配置对象，包含必要的默认值
    class DefaultConfig:
        VECTOR_STORE_TYPE = "qdrant"
        QDRANT_HOST = "localhost"
        QDRANT_PORT = 6333
        QDRANT_API_KEY = None
        QDRANT_COLLECTION_NAME = "papers"
        CHROMA_PERSIST_DIRECTORY = "./chroma_db"
        CHROMA_COLLECTION_NAME = "papers"
        EMBEDDING_DIMENSION = 384
    
    config = DefaultConfig()

logger = logging.getLogger(__name__)

class VectorStoreManager:
    """管理向量存储操作"""
    
    def __init__(self, embeddings):
        self.embeddings = embeddings
        self.vector_store = None
        self.client = None
        
    async def initialize(self):
        """初始化向量存储"""
        try:
            # 获取向量存储类型，默认使用qdrant
            vector_store_type = getattr(config, "VECTOR_STORE_TYPE", "qdrant")
            
            if vector_store_type == "qdrant":
                await self._initialize_qdrant()  # 使用await调用
            elif vector_store_type == "chroma":
                await self._initialize_chroma()  # 使用await调用
            else:
                raise ValueError(f"不支持的向量存储类型: {vector_store_type}")
                
            logger.info(f"向量存储初始化成功: {vector_store_type}")
            
        except Exception as e:
            logger.error(f"向量存储初始化失败: {str(e)}")
            raise
    
    async def _initialize_qdrant(self):
        """初始化Qdrant向量数据库"""
        try:
            # 创建Qdrant客户端，明确使用HTTP协议
            self.client = QdrantClient(
                host=getattr(config, "QDRANT_HOST", "localhost"),
                port=getattr(config, "QDRANT_PORT", 6333),
                api_key=getattr(config, "QDRANT_API_KEY", None),
                https=False  # 明确指定使用HTTP而不是HTTPS
            )
            
            # 修改：直接创建集合，如果已存在则忽略错误
            try:
                self.client.create_collection(
                    collection_name=getattr(config, "QDRANT_COLLECTION_NAME", "papers"),
                    vectors_config=models.VectorParams(
                        size=getattr(config, "EMBEDDING_DIMENSION", 384),
                        distance=models.Distance.COSINE
                    )
                )
                logger.info(f"创建了新的Qdrant集合: {getattr(config, 'QDRANT_COLLECTION_NAME', 'papers')}")
            except Exception as e:
                # 检查错误是否因为集合已存在
                if "already exists" in str(e):
                    logger.info(f"Qdrant集合 {getattr(config, 'QDRANT_COLLECTION_NAME', 'papers')} 已存在")
                else:
                    # 其他错误则记录警告
                    logger.warning(f"创建集合时出现问题: {str(e)}")
            
            # 创建LangChain Qdrant向量存储
            self.vector_store = Qdrant(
                client=self.client,
                collection_name=getattr(config, "QDRANT_COLLECTION_NAME", "papers"),
                embeddings=self.embeddings
            )
            
        except Exception as e:
            logger.error(f"Qdrant初始化失败: {str(e)}")
            raise
    
    async def _initialize_chroma(self):
        """初始化ChromaDB向量数据库"""
        try:
            from langchain_community.vectorstores import Chroma
            
            self.vector_store = Chroma(
                collection_name=getattr(config, "CHROMA_COLLECTION_NAME", "papers"),
                embedding_function=self.embeddings,
                persist_directory=getattr(config, "CHROMA_PERSIST_DIRECTORY", "./chroma_db")
            )
            
        except ImportError:
            logger.error("ChromaDB未安装，请运行: pip install chromadb")
            raise
        except Exception as e:
            logger.error(f"ChromaDB初始化失败: {str(e)}")
            raise
    
    async def _search_qdrant(self, query: str, k: int = 5, filter_dict: Optional[Dict] = None) -> List[Document]:
        """在Qdrant中搜索文档"""
        try:
            if not self.vector_store:
                raise RuntimeError("Qdrant向量存储未初始化")
            
            # 转换为Qdrant过滤器格式
            qdrant_filter = None
            if filter_dict:
                conditions = []
                for key, value in filter_dict.items():
                    conditions.append(models.FieldCondition(
                        key=f"metadata.{key}",
                        match=models.MatchValue(value=value)
                    ))
                if conditions:
                    qdrant_filter = models.Filter(must=conditions)
            
            # 进行搜索，使用字典形式传递参数，避免位置参数错误
            if qdrant_filter:
                docs = self.vector_store.similarity_search(
                    query, 
                    k=k,
                    filter=qdrant_filter
                )
            else:
                docs = self.vector_store.similarity_search(
                    query,
                    k=k
                )
            
            return docs
            
        except Exception as e:
            logger.error(f"Qdrant搜索失败: {str(e)}")
            return []
    
    async def _search_chroma(self, query: str, k: int = 5, filter_dict: Optional[Dict] = None) -> List[Document]:
        """在ChromaDB中搜索文档"""
        try:
            if not self.vector_store:
                raise RuntimeError("ChromaDB向量存储未初始化")
            
            # 转换为ChromaDB过滤器格式
            chroma_filter = None
            if filter_dict:
                # ChromaDB使用不同的过滤格式
                chroma_filter = {}
                for key, value in filter_dict.items():
                    chroma_filter[f"metadata.{key}"] = value
            
            # 进行搜索，使用字典形式传递参数
            if chroma_filter:
                docs = self.vector_store.similarity_search(
                    query,
                    k=k,
                    filter=chroma_filter
                )
            else:
                docs = self.vector_store.similarity_search(
                    query,
                    k=k
                )
            
            return docs
            
        except Exception as e:
            logger.error(f"ChromaDB搜索失败: {str(e)}")
            return []
    
    async def add_documents(self, documents: List[Document]) -> List[str]:
        """添加文档到向量存储"""
        try:
            if not self.vector_store:
                raise RuntimeError("向量存储未初始化")
            
            # 添加文档并获取ID
            doc_ids = self.vector_store.add_documents(documents)
            
            logger.info(f"成功添加 {len(documents)} 个文档到向量存储")
            return doc_ids
            
        except Exception as e:
            logger.error(f"添加文档失败: {str(e)}")
            raise
    
    async def search_documents(self, query: str, k: int = 5, filter_dict: Optional[Dict] = None) -> List[Document]:
        """
        使用向量搜索查询相关文档
        
        参数:
            query: 查询文本
            k: 返回的最大文档数
            filter_dict: 过滤条件，如{'paper_id': 123}
            
        返回:
            相关文档列表
        """
        try:
            logging.info(f"Searching for documents with query: {query[:50]}... (k={k})")
            
            # 支持按paper_id搜索的特殊情况
            if query.startswith('paper_id_'):
                try:
                    paper_id = int(query.split('_')[-1])
                    logging.info(f"Special case - searching by paper_id: {paper_id}")
                    filter_dict = filter_dict or {}
                    filter_dict['paper_id'] = paper_id
                    # 使用一个通用查询，因为我们是按元数据过滤
                    query = "academic paper content"
                except (ValueError, IndexError):
                    logging.warning(f"Invalid paper_id format in query: {query}")
            
            # 使用向量存储搜索
            if self.vector_store is None:
                # 修正：初始化方法现在是异步的，需要await调用
                await self.initialize()
            
            if filter_dict:
                logging.info(f"Applying filter: {filter_dict}")
            
            # 获取向量存储类型，默认使用qdrant
            vector_store_type = getattr(config, "VECTOR_STORE_TYPE", "qdrant")
            
            results = []  # 初始化结果变量，确保在所有路径中都有值
            
            if vector_store_type == "qdrant":
                results = await self._search_qdrant(query, k, filter_dict)
            elif vector_store_type == "chroma":
                results = await self._search_chroma(query, k, filter_dict)
            else:
                logging.error(f"Unsupported vector store type: {vector_store_type}")
                return []
            
            logging.info(f"Found {len(results)} matching documents")
            return results
            
        except Exception as e:
            logging.error(f"Error searching documents: {str(e)}")
            return []
    
    async def search_with_scores(self, query: str, k: int = 5) -> List[tuple]:
        """搜索文档并返回相似度分数"""
        try:
            if not self.vector_store:
                raise RuntimeError("向量存储未初始化")
            
            docs_with_scores = self.vector_store.similarity_search_with_score(query, k=k)
            
            logger.info(f"检索到 {len(docs_with_scores)} 个文档（带分数）")
            return docs_with_scores
            
        except Exception as e:
            logger.error(f"文档搜索失败: {str(e)}")
            raise
    
    def get_retriever(self, k: int = 5, search_type: str = "similarity") -> Any:
        """获取检索器用于RAG链"""
        if not self.vector_store:
            logger.warning("向量存储未初始化，返回Mock检索器")
            return MockRetriever(k=k)
        
        return self.vector_store.as_retriever(
            search_type=search_type,
            search_kwargs={"k": k}
        )
    
    async def delete_documents(self, doc_ids: List[str]) -> bool:
        """删除指定文档"""
        try:
            if not self.vector_store:
                raise RuntimeError("向量存储未初始化")
            
            if hasattr(self.vector_store, 'delete'):
                self.vector_store.delete(doc_ids)
                logger.info(f"删除了 {len(doc_ids)} 个文档")
                return True
            else:
                logger.warning("当前向量存储不支持删除操作")
                return False
                
        except Exception as e:
            logger.error(f"删除文档失败: {str(e)}")
            raise
    
    async def get_collection_info(self) -> Dict[str, Any]:
        """获取集合信息"""
        try:
            # 获取向量存储类型，默认使用qdrant
            vector_store_type = getattr(config, "VECTOR_STORE_TYPE", "qdrant")
            
            if vector_store_type == "qdrant" and self.client:
                info = self.client.get_collection(getattr(config, "QDRANT_COLLECTION_NAME", "papers"))
                return {
                    "name": getattr(config, "QDRANT_COLLECTION_NAME", "papers"),  # 直接使用配置中的名称
                    "vector_count": info.vectors_count,
                    "status": str(info.status)
                }
            else:
                return {"message": "集合信息获取功能仅支持Qdrant"}
                
        except Exception as e:
            logger.error(f"获取集合信息失败: {str(e)}")
            return {"error": str(e)}
    
    async def cleanup(self):
        """清理资源"""
        try:
            if self.client:
                self.client.close()
            logger.info("向量存储资源清理完成")
        except Exception as e:
            logger.warning(f"清理资源时出错: {str(e)}")


class MockRetriever:
    """Mock检索器，用于测试或fallback"""
    
    def __init__(self, k: int = 5):
        self.k = k
        logger.warning("使用Mock检索器 - 仅用于测试")
    
    def get_relevant_documents(self, query: str) -> List[Document]:
        """Mock方法，返回示例文档"""
        return [
            Document(
                page_content=f"这是关于'{query}'的模拟文档内容...",
                metadata={
                    "source": "mock",
                    "paper_id": "mock_paper_001",
                    "title": "模拟论文标题"
                }
            )
        ]
    
    async def aget_relevant_documents(self, query: str) -> List[Document]:
        """异步版本的Mock方法"""
        return self.get_relevant_documents(query) 