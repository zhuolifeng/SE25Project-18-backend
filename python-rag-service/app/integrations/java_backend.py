"""
Java Backend Integration Client
Handles communication with the Java Spring Boot backend
"""


import aiohttp
import asyncio
from typing import Dict, Any, List, Optional
import logging
from urllib.parse import urljoin


logger = logging.getLogger(__name__)

class JavaBackendClient:
    """Client for interacting with Java backend API"""
    
    def __init__(self, base_url: str, api_key: Optional[str] = None):
        self.base_url = base_url.rstrip('/')
        self.api_key = api_key
        self.session = None
        
    async def _get_session(self) -> aiohttp.ClientSession:
        """Get or create aiohttp session"""
        if self.session is None or self.session.closed:
            headers = {}
            if self.api_key:
                headers['Authorization'] = f'Bearer {self.api_key}'
            self.session = aiohttp.ClientSession(headers=headers)
        return self.session
    
    async def get_paper(self, paper_id: str) -> Dict[str, Any]:
        """Fetch paper details from Java backend"""
        try:
            session = await self._get_session()
            url = urljoin(self.base_url, f'/api/papers/{paper_id}')
            
            async with session.get(url) as response:
                if response.status == 200:
                    return await response.json()
                else:
                    error_text = await response.text()
                    raise Exception(f"Failed to fetch paper {paper_id}: {error_text}")
                    
        except Exception as e:
            logger.error(f"Error fetching paper {paper_id}: {str(e)}")
            raise
    
    async def get_papers_batch(self, paper_ids: List[str]) -> List[Dict[str, Any]]:
        """Fetch multiple papers in parallel"""
        tasks = [self.get_paper(paper_id) for paper_id in paper_ids]
        results = await asyncio.gather(*tasks, return_exceptions=True)
        
        papers = []
        for i, result in enumerate(results):
            if isinstance(result, Exception):
                logger.error(f"Failed to fetch paper {paper_ids[i]}: {str(result)}")
            else:
                papers.append(result)
        
        return papers
    
    async def search_papers(self, search_dto: Dict[str, Any]) -> List[Dict[str, Any]]:
        """Search papers using Java backend search API"""
        try:
            session = await self._get_session()
            url = urljoin(self.base_url, '/api/papers/search')
            
            async with session.post(url, json=search_dto) as response:
                if response.status == 200:
                    return await response.json()
                else:
                    error_text = await response.text()
                    raise Exception(f"Failed to search papers: {error_text}")
                    
        except Exception as e:
            logger.error(f"Error searching papers: {str(e)}")
            raise
    
    async def log_search_history(self, user_id: str, search_term: str) -> None:
        """Log user search history to Java backend"""
        try:
            session = await self._get_session()
            url = urljoin(self.base_url, '/api/users/history/search')
            
            data = {
                "userId": user_id,
                "searchTerm": search_term
            }
            
            async with session.post(url, json=data) as response:
                if response.status != 200:
                    error_text = await response.text()
                    logger.warning(f"Failed to log search history: {error_text}")
                    
        except Exception as e:
            logger.warning(f"Error logging search history: {str(e)}")
            # Don't raise - this is not critical
    
    async def log_view_history(self, user_id: str, paper_id: str) -> None:
        """Log user paper view history to Java backend"""
        try:
            session = await self._get_session()
            url = urljoin(self.base_url, '/api/users/history/view')
            
            data = {
                "userId": user_id,
                "paperId": paper_id
            }
            
            async with session.post(url, json=data) as response:
                if response.status != 200:
                    error_text = await response.text()
                    logger.warning(f"Failed to log view history: {error_text}")
                    
        except Exception as e:
            logger.warning(f"Error logging view history: {str(e)}")
            # Don't raise - this is not critical
    
    async def get_user_info(self, user_id: str) -> Optional[Dict[str, Any]]:
        """Get user information from Java backend"""
        try:
            session = await self._get_session()
            url = urljoin(self.base_url, f'/api/users/{user_id}')
            
            async with session.get(url) as response:
                if response.status == 200:
                    return await response.json()
                else:
                    return None
                    
        except Exception as e:
            logger.error(f"Error fetching user info: {str(e)}")
            return None
    
    async def close(self):
        """Close the HTTP session"""
        if self.session and not self.session.closed:
            await self.session.close()