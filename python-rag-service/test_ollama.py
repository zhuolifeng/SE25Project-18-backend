#!/usr/bin/env python
"""
测试Ollama连接和查询功能
这个脚本用于验证Ollama配置是否正确，并测试基本查询功能。
"""

import os
import sys
import time
import asyncio
import logging
from typing import Optional

# 添加项目根目录到Python路径
root_path = os.path.abspath(os.path.dirname(__file__))
sys.path.insert(0, root_path)

from config import config, configure_logging

# 配置日志
configure_logging()
logger = logging.getLogger(__name__)

async def test_ollama_connection():
    """测试Ollama连接"""
    try:
        from langchain_community.llms import Ollama
        
        logger.info("正在测试Ollama连接...")
        
        # 获取配置
        ollama_base_url = getattr(config, "OLLAMA_BASE_URL", "http://localhost:11434")
        ollama_model = getattr(config, "LLM_MODEL", "llama2")
        
        logger.info(f"Ollama配置: URL={ollama_base_url}, 模型={ollama_model}")
        
        # 创建Ollama实例
        ollama = Ollama(
            base_url=ollama_base_url,
            model=ollama_model,
            temperature=0.7,
            timeout=30  # 短超时用于连接测试
        )
        
        # 测试连接 - 简单查询
        logger.info("发送测试查询...")
        start_time = time.time()
        response = await ollama.ainvoke("你好，这是一个测试。")
        end_time = time.time()
        
        logger.info(f"Ollama响应: {response[:100]}..." if len(response) > 100 else f"Ollama响应: {response}")
        logger.info(f"查询耗时: {end_time - start_time:.2f}秒")
        
        return True, "Ollama连接和查询测试成功"
        
    except ImportError:
        logger.error("缺少langchain_community包。运行: pip install langchain-community")
        return False, "缺少langchain_community包"
    except Exception as e:
        logger.error(f"Ollama测试失败: {str(e)}")
        return False, f"Ollama测试失败: {str(e)}"

async def test_ollama_rag():
    """测试Ollama与RAG引擎集成"""
    try:
        # 仅导入需要的组件
        from app.core.llm_provider import LLMProvider
        
        logger.info("测试Ollama与RAG引擎集成...")
        
        # 创建LLM提供者
        llm_provider = LLMProvider()
        
        # 检查是否使用Ollama
        if getattr(config, "LLM_PROVIDER", "").lower() != "ollama":
            logger.warning(f"配置的LLM提供商不是Ollama，当前为: {getattr(config, 'LLM_PROVIDER', 'unknown')}")
            logger.warning("继续测试，但会强制使用Ollama...")
        
        # 直接创建Ollama LLM
        llm = llm_provider._create_ollama_llm()
        
        # 测试查询
        test_prompt = "解释什么是向量数据库以及它在RAG系统中的作用"
        logger.info(f"向Ollama发送查询: {test_prompt}")
        
        start_time = time.time()
        response = llm.invoke(test_prompt)
        end_time = time.time()
        
        logger.info(f"Ollama回复: {str(response)[:200]}..." if len(str(response)) > 200 else f"Ollama回复: {str(response)}")
        logger.info(f"查询耗时: {end_time - start_time:.2f}秒")
        
        return True, "Ollama RAG集成测试成功"
        
    except Exception as e:
        logger.error(f"Ollama RAG测试失败: {str(e)}")
        return False, f"Ollama RAG测试失败: {str(e)}"

async def list_ollama_models():
    """列出可用的Ollama模型"""
    try:
        import requests
        
        ollama_base_url = getattr(config, "OLLAMA_BASE_URL", "http://localhost:11434")
        
        logger.info(f"获取Ollama可用模型列表...")
        response = requests.get(f"{ollama_base_url}/api/tags")
        
        if response.status_code == 200:
            models = response.json().get("models", [])
            logger.info(f"可用模型列表 ({len(models)}):")
            for model in models:
                model_name = model.get("name", "unknown")
                model_size = model.get("size", 0) / 1024 / 1024 / 1024  # 转换为GB
                logger.info(f"- {model_name} ({model_size:.2f} GB)")
            return True, models
        else:
            logger.warning(f"无法获取模型列表，HTTP状态码: {response.status_code}")
            return False, []
            
    except ImportError:
        logger.error("缺少requests包。运行: pip install requests")
        return False, []
    except Exception as e:
        logger.error(f"获取Ollama模型列表失败: {str(e)}")
        return False, []

async def main():
    """主函数"""
    print("\n====== Ollama 连接测试 ======\n")
    
    connection_success, connection_message = await test_ollama_connection()
    print(f"\n连接测试结果: {'✓ 成功' if connection_success else '✗ 失败'}")
    print(f"详情: {connection_message}")
    
    if connection_success:
        print("\n====== Ollama 模型列表 ======\n")
        models_success, models = await list_ollama_models()
        
        print("\n====== Ollama RAG集成测试 ======\n")
        rag_success, rag_message = await test_ollama_rag()
        print(f"\nRAG集成测试结果: {'✓ 成功' if rag_success else '✗ 失败'}")
        print(f"详情: {rag_message}")
    
    print("\n测试完成！")

if __name__ == "__main__":
    asyncio.run(main()) 