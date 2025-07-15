"""
Vector Store Manager
Handles vector database operations for document storage and retrieval
"""

from typing import List, Optional, Dict, Any
import logging
from langchain_community.vectorstores import Qdrant
from langchain.schema import Document
from qdrant_client import QdrantClient
from qdrant_client.http import models
from app.core.config import settings

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
            if settings.VECTOR_STORE_TYPE == "qdrant":
                await self._initialize_qdrant()
            elif settings.VECTOR_STORE_TYPE == "chroma":
                await self._initialize_chroma()
            else:
                raise ValueError(f"不支持的向量存储类型: {settings.VECTOR_STORE_TYPE}")
                
            logger.info(f"向量存储初始化成功: {settings.VECTOR_STORE_TYPE}")
            
        except Exception as e:
            logger.error(f"向量存储初始化失败: {str(e)}")
            raise
    
    async def _initialize_qdrant(self):
        """初始化Qdrant向量数据库"""
        try:
            # 创建Qdrant客户端，明确使用HTTP协议
            self.client = QdrantClient(
                host=settings.QDRANT_HOST,
                port=settings.QDRANT_PORT,
                api_key=settings.QDRANT_API_KEY,
                https=False  # 明确指定使用HTTP而不是HTTPS
            )
            
            # 检查集合是否存在，不存在则创建
            collections = self.client.get_collections()
            collection_names = [col.name for col in collections.collections]
            
            if settings.QDRANT_COLLECTION_NAME not in collection_names:
                self.client.create_collection(
                    collection_name=settings.QDRANT_COLLECTION_NAME,
                    vectors_config=models.VectorParams(
                        size=settings.EMBEDDING_DIMENSION,
                        distance=models.Distance.COSINE
                    )
                )
                logger.info(f"创建Qdrant集合: {settings.QDRANT_COLLECTION_NAME}")
            
            # 创建LangChain Qdrant向量存储
            self.vector_store = Qdrant(
                client=self.client,
                collection_name=settings.QDRANT_COLLECTION_NAME,
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
                collection_name=settings.CHROMA_COLLECTION_NAME,
                embedding_function=self.embeddings,
                persist_directory=settings.CHROMA_PERSIST_DIRECTORY
            )
            
        except ImportError:
            logger.error("ChromaDB未安装，请运行: pip install chromadb")
            raise
        except Exception as e:
            logger.error(f"ChromaDB初始化失败: {str(e)}")
            raise
    
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
        """搜索相似文档"""
        try:
            if not self.vector_store:
                raise RuntimeError("向量存储未初始化")
            
            if filter_dict:
                # 带过滤条件的搜索
                docs = self.vector_store.similarity_search(
                    query, k=k, filter=filter_dict
                )
            else:
                # 普通相似性搜索
                docs = self.vector_store.similarity_search(query, k=k)
            
            logger.info(f"检索到 {len(docs)} 个相关文档")
            return docs
            
        except Exception as e:
            logger.error(f"文档搜索失败: {str(e)}")
            raise
    
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
            if settings.VECTOR_STORE_TYPE == "qdrant" and self.client:
                info = self.client.get_collection(settings.QDRANT_COLLECTION_NAME)
                return {
                    "name": info.config.name,
                    "vector_count": info.vectors_count,
                    "status": info.status
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