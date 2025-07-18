"""
LLM Provider Manager
支持阿里云通义千问（Qwen）API和本地Ollama模型
"""

import logging
import os
from typing import Optional
import sys
import requests
import time

# 添加项目根目录到Python路径
root_path = os.path.abspath(os.path.join(os.path.dirname(__file__), '..', '..'))
sys.path.insert(0, root_path)

# 尝试导入配置，如果失败则使用默认值
try:
    from config import config
except (ImportError, AttributeError) as e:
    logging.warning(f"无法导入配置，使用默认值: {str(e)}")
    # 定义一个简单的配置对象，包含必要的默认值
    class DefaultConfig:
        LLM_PROVIDER = "qwen"
        LLM_MODEL = "qwen-turbo"
        QWEN_API_KEY = None
        OLLAMA_BASE_URL = "http://localhost:11434"
        OLLAMA_TIMEOUT = 120
        OLLAMA_RETRY_COUNT = 3
        OLLAMA_RETRY_DELAY = 2
        LLM_TEMPERATURE = 0.7
        LLM_MAX_TOKENS = 2000
    
    config = DefaultConfig()

logger = logging.getLogger(__name__)

class LLMProvider:
    """管理LLM提供商连接，支持Qwen和Ollama"""
    
    def __init__(self):
        self.llm = None
        
    def get_llm(self):
        """获取LLM实例"""
        if self.llm is None:
            self.llm = self._create_llm()
        return self.llm
    
    def _create_llm(self):
        """创建LLM实例"""
        provider = getattr(config, "LLM_PROVIDER", "qwen").lower()
        
        try:
            if provider == "qwen":
                return self._create_qwen_llm()
            elif provider == "ollama":
                return self._create_ollama_llm()
            else:
                logger.warning(f"不支持的LLM提供商: {provider}，使用Mock LLM")
                return self._create_mock_llm()
        except Exception as e:
            logger.error(f"创建LLM时出错: {e}")
            logger.info("回退到Mock LLM")
            return self._create_mock_llm()
    
    def _create_qwen_llm(self):
        """创建Qwen LLM实例使用阿里云API"""
        if not getattr(config, "QWEN_API_KEY", None):
            raise ValueError("QWEN_API_KEY is required for Qwen provider")
        
        try:
            # 设置环境变量给dashscope使用
            os.environ["DASHSCOPE_API_KEY"] = getattr(config, "QWEN_API_KEY", "")
            
            from langchain_community.llms import Tongyi
            
            llm = Tongyi(
                model_name=getattr(config, "LLM_MODEL", "qwen-turbo"),
                temperature=getattr(config, "LLM_TEMPERATURE", 0.7),
                max_tokens=getattr(config, "LLM_MAX_TOKENS", 2000),
                top_p=0.8
            )
            
            logger.info(f"成功初始化Qwen LLM，使用模型: {getattr(config, 'LLM_MODEL', 'qwen-turbo')}")
            return llm
            
        except ImportError:
            logger.error("缺少langchain_community或dashscope包。运行: pip install langchain-community dashscope")
            raise
        except Exception as e:
            logger.error(f"初始化Qwen LLM失败: {e}")
            raise
    
    def _check_ollama_connection(self, base_url, retry_count=3, retry_delay=2):
        """检查到Ollama服务器的连接"""
        logger.info(f"检查Ollama服务器连接: {base_url}")
        
        for attempt in range(retry_count):
            try:
                response = requests.get(f"{base_url}/api/version", timeout=10)
                if response.status_code == 200:
                    version_info = response.json()
                    logger.info(f"Ollama服务器连接成功，版本: {version_info.get('version', '未知')}")
                    return True
                else:
                    logger.warning(f"Ollama服务器返回非200状态码: {response.status_code}")
            except requests.exceptions.RequestException as e:
                logger.warning(f"连接Ollama服务器失败 (尝试 {attempt+1}/{retry_count}): {str(e)}")
                if attempt < retry_count - 1:
                    logger.info(f"等待 {retry_delay} 秒后重试...")
                    time.sleep(retry_delay)
        
        logger.error(f"无法连接到Ollama服务器: {base_url}")
        return False
    
    def _check_model_availability(self, base_url, model_name):
        """检查指定的模型是否可用"""
        try:
            response = requests.get(f"{base_url}/api/tags", timeout=10)
            if response.status_code == 200:
                models = response.json().get("models", [])
                available_models = [model.get("name") for model in models]
                
                if model_name in available_models:
                    logger.info(f"模型 '{model_name}' 在Ollama服务器上可用")
                    return True
                else:
                    logger.warning(f"模型 '{model_name}' 在Ollama服务器上不可用")
                    logger.info(f"可用模型: {', '.join(available_models) if available_models else '无'}")
                    return False
            else:
                logger.warning("获取可用模型列表失败")
                return False
        except Exception as e:
            logger.warning(f"检查模型可用性时出错: {str(e)}")
            return False
    
    def _create_ollama_llm(self):
        """创建Ollama LLM实例连接本地或远程Ollama服务"""
        try:
            # 使用新的langchain_ollama包
            from langchain_ollama import OllamaLLM
            
            # 获取配置
            ollama_base_url = getattr(config, "OLLAMA_BASE_URL", "http://localhost:11434")
            ollama_model = getattr(config, "LLM_MODEL", "qwen2.5-math:latest")
            ollama_timeout = getattr(config, "OLLAMA_TIMEOUT", 120)
            
            # 创建LLM实例
            llm = OllamaLLM(
                base_url=ollama_base_url,
                model=ollama_model,
                temperature=getattr(config, "LLM_TEMPERATURE", 0.7),
                num_predict=getattr(config, "LLM_MAX_TOKENS", 2000),
                timeout=ollama_timeout
            )
            
            logger.info(f"成功初始化Ollama LLM，使用模型: {ollama_model}")
            return llm
        
        except ImportError:
            logger.error("缺少langchain_ollama包。运行: pip install langchain-ollama")
            raise
        except Exception as e:
            logger.error(f"初始化Ollama LLM失败: {e}")
            raise
    
    def _create_mock_llm(self):
        """创建Mock LLM用于测试"""
        logger.warning("使用Mock LLM - 仅用于测试")
        return MockLLM()


class MockLLM:
    """测试用的Mock LLM"""
    
    def __init__(self):
        self.model_name = "mock-llm"
    
    def invoke(self, input_text, config=None):
        """Mock invoke方法，兼容LangChain接口"""
        if isinstance(input_text, list):
            # 处理消息列表格式 (ChatPromptValue format)
            content = str(input_text[-1]) if input_text else "No input"
        else:
            content = str(input_text)
        
        return MockResponse(f"这是Mock LLM的回复: {content[:100]}...")
    
    def __call__(self, input_text, **kwargs):
        """Mock call方法，向后兼容"""
        return self.invoke(input_text)


class MockResponse:
    """Mock响应对象，兼容LangChain响应格式"""
    
    def __init__(self, content: str):
        self.content = content
    
    def __str__(self):
        return self.content 