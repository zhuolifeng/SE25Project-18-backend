"""
LLM Provider Manager
只支持阿里云通义千问（Qwen）API
"""

import logging
import os
from typing import Optional
import sys

# 添加项目根目录到Python路径
root_path = os.path.abspath(os.path.join(os.path.dirname(__file__), '..', '..'))
sys.path.insert(0, root_path)

from config import config

logger = logging.getLogger(__name__)

class LLMProvider:
    """管理LLM提供商连接，只支持Qwen"""
    
    def __init__(self):
        self.llm = None
        
    def get_llm(self):
        """获取LLM实例"""
        if self.llm is None:
            self.llm = self._create_llm()
        return self.llm
    
    def _create_llm(self):
        """创建LLM实例"""
        try:
            return self._create_qwen_llm()
        except Exception as e:
            logger.error(f"Error creating Qwen LLM: {e}")
            logger.info("Falling back to mock LLM")
            return self._create_mock_llm()
    
    def _create_qwen_llm(self):
        """创建Qwen LLM实例使用阿里云API"""
        if not config.QWEN_API_KEY:
            raise ValueError("QWEN_API_KEY is required for Qwen provider")
        
        try:
            # 设置环境变量给dashscope使用
            os.environ["DASHSCOPE_API_KEY"] = config.QWEN_API_KEY
            
            from langchain_community.llms import Tongyi
            
            llm = Tongyi(
                model_name=config.LLM_MODEL or "qwen-turbo",
                temperature=config.LLM_TEMPERATURE,
                max_tokens=config.LLM_MAX_TOKENS,
                top_p=0.8
            )
            
            logger.info(f"Successfully initialized Qwen LLM with model: {config.LLM_MODEL}")
            return llm
            
        except ImportError:
            logger.error("langchain_community or dashscope not installed. Run: pip install langchain-community dashscope")
            raise
        except Exception as e:
            logger.error(f"Failed to initialize Qwen LLM: {e}")
            raise
    
    def _create_mock_llm(self):
        """创建Mock LLM用于测试"""
        logger.warning("Using mock LLM - this is for testing only")
        return MockLLM()


class MockLLM:
    """测试用的Mock LLM"""
    
    def __init__(self):
        self.model_name = "mock-qwen"
    
    def invoke(self, input_text, config=None):
        """Mock invoke方法，兼容LangChain接口"""
        if isinstance(input_text, list):
            # 处理消息列表格式 (ChatPromptValue format)
            content = str(input_text[-1]) if input_text else "No input"
        else:
            content = str(input_text)
        
        return MockResponse(f"这是Mock Qwen的回复: {content[:100]}...")
    
    def __call__(self, input_text, **kwargs):
        """Mock call方法，向后兼容"""
        return self.invoke(input_text)


class MockResponse:
    """Mock响应对象，兼容LangChain响应格式"""
    
    def __init__(self, content: str):
        self.content = content
    
    def __str__(self):
        return self.content 