#!/usr/bin/env python3
"""
调试启动脚本 - 逐步测试RAG引擎初始化
"""

import asyncio
import logging
from dotenv import load_dotenv

# 设置日志
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

# 加载环境变量
load_dotenv()

async def test_step_by_step():
    """逐步测试各个组件"""
    print("=== 开始逐步调试 ===")
    
    try:
        # 步骤1: 测试配置加载
        print("\n1. 测试配置加载...")
        from app.core.config import settings
        print(f"   ✅ 配置加载成功")
        print(f"   QWEN_API_KEY: {'已设置' if settings.QWEN_API_KEY else '未设置'}")
        print(f"   LLM_MODEL: {settings.LLM_MODEL}")
        
        # 步骤2: 测试嵌入模型
        print("\n2. 测试嵌入模型...")
        from langchain_community.embeddings import HuggingFaceEmbeddings
        embeddings = HuggingFaceEmbeddings(
            model_name=settings.EMBEDDING_MODEL,
            model_kwargs={'device': 'cpu'},
            encode_kwargs={'normalize_embeddings': True}
        )
        print("   ✅ 嵌入模型加载成功")
        
        # 步骤3: 测试向量存储
        print("\n3. 测试向量存储...")
        from app.core.vector_store import VectorStoreManager
        vector_store = VectorStoreManager(embeddings)
        await vector_store.initialize()
        print("   ✅ 向量存储初始化成功")
        
        # 步骤4: 测试LLM提供商
        print("\n4. 测试LLM提供商...")
        from app.core.llm_provider import LLMProvider
        llm_provider = LLMProvider()
        llm = llm_provider.get_llm()
        print("   ✅ LLM提供商初始化成功")
        
        # 步骤5: 测试Java后端客户端
        print("\n5. 测试Java后端客户端...")
        from app.integrations.java_backend import JavaBackendClient
        java_client = JavaBackendClient(settings.JAVA_BACKEND_URL)
        print("   ✅ Java后端客户端初始化成功")
        
        # 步骤6: 测试简单查询
        print("\n6. 测试简单查询...")
        test_result = llm.invoke("你好")
        print(f"   ✅ LLM查询成功: {str(test_result)[:100]}...")
        
        print("\n=== 所有组件测试通过 ===")
        return True
        
    except Exception as e:
        print(f"\n❌ 测试失败: {str(e)}")
        import traceback
        traceback.print_exc()
        return False

if __name__ == "__main__":
    success = asyncio.run(test_step_by_step())
    if success:
        print("\n🎉 所有组件正常，可以启动服务")
    else:
        print("\n🔧 请修复上述错误后再启动服务") 