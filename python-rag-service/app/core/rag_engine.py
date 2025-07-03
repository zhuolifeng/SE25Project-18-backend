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

from app.core.config import settings
from app.core.vector_store import VectorStoreManager
from app.core.llm_provider import LLMProvider
from app.integrations.java_backend import JavaBackendClient

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
        try:
            # Initialize embeddings
            logger.info(f"Loading embedding model: {settings.EMBEDDING_MODEL}")
            self.embeddings = HuggingFaceEmbeddings(
                model_name=settings.EMBEDDING_MODEL,
                model_kwargs={'device': 'cpu'},
                encode_kwargs={'normalize_embeddings': True}
            )
            
            # Initialize text splitter
            self.text_splitter = RecursiveCharacterTextSplitter(
                chunk_size=settings.RAG_CHUNK_SIZE,
                chunk_overlap=settings.RAG_CHUNK_OVERLAP,
                length_function=len,
                separators=["\n\n", "\n", ".", " ", ""]
            )
            
            # Initialize vector store
            self.vector_store_manager = VectorStoreManager(self.embeddings)
            await self.vector_store_manager.initialize()
            
            # Initialize LLM provider
            self.llm_provider = LLMProvider()
            
            # Initialize Java backend client
            self.java_client = JavaBackendClient(settings.JAVA_BACKEND_URL)
            
            # Initialize conversation memory
            self.memory = ConversationBufferMemory(
                memory_key="chat_history",
                return_messages=True,
                output_key="answer"
            )
            
            # Create QA chain
            self._create_qa_chain()
            
            logger.info("RAG Engine initialized successfully")
            
        except Exception as e:
            logger.error(f"Failed to initialize RAG Engine: {str(e)}")
            raise
            
    def _create_qa_chain(self):
        """Create the conversational retrieval chain"""
        self.qa_chain = ConversationalRetrievalChain.from_llm(
            llm=self.llm_provider.get_llm(),
            retriever=self.vector_store_manager.get_retriever(k=settings.RAG_TOP_K),
            memory=self.memory,
            return_source_documents=True,
            verbose=settings.DEBUG_MODE
        )
    
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
        """Process a query and return answer with sources"""
        try:
            # Log search history if user_id provided
            if user_id:
                await self.java_client.log_search_history(user_id, question)
            
            # If paper_id is provided, log view history
            if user_id and paper_id:
                await self.java_client.log_view_history(user_id, paper_id)
            
            # Process conversation history if provided
            if history:
                # Add conversation history to memory
                for item in history:
                    if item.get("role") == "user":
                        self.memory.chat_memory.add_user_message(item.get("content", ""))
                    elif item.get("role") == "assistant":
                        self.memory.chat_memory.add_ai_message(item.get("content", ""))
            
            # Get answer from QA chain
            result = await asyncio.to_thread(
                self.qa_chain,
                {"question": question}
            )
            
            # Extract source documents
            sources = []
            if "source_documents" in result:
                for doc in result["source_documents"]:
                    sources.append({
                        "paper_id": doc.metadata.get("paper_id"),
                        "title": doc.metadata.get("title"),
                        "authors": doc.metadata.get("authors"),
                        "chunk": doc.page_content[:200] + "..."
                    })
            
            return {
                "answer": result["answer"],
                "sources": sources,
                "question": question
            }
            
        except Exception as e:
            logger.error(f"Failed to process query: {str(e)}")
            raise
    
    async def clear_conversation(self, session_id: Optional[str] = None):
        """Clear conversation memory"""
        self.memory.clear()
        
    async def cleanup(self):
        """Cleanup resources"""
        if self.vector_store_manager:
            await self.vector_store_manager.cleanup() 