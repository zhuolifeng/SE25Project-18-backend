#!/usr/bin/env python3
"""
Qwen LLM Integration Test Script
测试阿里云通义千问（Qwen）API集成是否正常工作
"""

import os
import sys
import asyncio
from dotenv import load_dotenv

# 加载环境变量
load_dotenv()

# 添加项目路径
sys.path.append(os.path.dirname(os.path.abspath(__file__)))

from app.core.config import settings
from app.core.llm_provider import LLMProvider

async def test_qwen():
    """测试Qwen API"""
    print("=== 测试阿里云通义千问（Qwen）API ===")
    
    try:
        if not settings.QWEN_API_KEY:
            print("❌ QWEN_API_KEY未设置，请在.env文件中设置您的阿里云API密钥")
            print("获取方式：https://dashscope.console.aliyun.com/")
            return
            
        provider = LLMProvider()
        llm = provider.get_llm()
        
        # 测试中文问答
        question = "什么是机器学习？请简单介绍一下。"
        print(f"问题: {question}")
        
        response = llm.invoke(question)
        print(f"回答: {response}")
        print("✅ Qwen API测试成功")
        
        # 测试英文问答
        question_en = "What is artificial intelligence?"
        print(f"\n问题: {question_en}")
        
        response_en = llm.invoke(question_en)
        print(f"回答: {response_en}")
        print("✅ Qwen英文测试成功")
        
    except Exception as e:
        print(f"❌ Qwen API测试失败: {e}")
        print("请检查：")
        print("1. QWEN_API_KEY是否正确设置")
        print("2. 网络连接是否正常")
        print("3. 是否安装了dashscope: pip install dashscope")

async def test_mock():
    """测试Mock LLM"""
    print("\n=== 测试Mock LLM ===")
    
    # 临时设置无效的API密钥来触发Mock模式
    original_api_key = settings.QWEN_API_KEY
    settings.QWEN_API_KEY = None
    
    try:
        provider = LLMProvider()
        llm = provider.get_llm()
        
        # 测试简单问答
        question = "Hello, world!"
        print(f"问题: {question}")
        
        response = llm.invoke(question)
        print(f"回答: {response}")
        print("✅ Mock LLM测试成功")
        
    except Exception as e:
        print(f"❌ Mock LLM测试失败: {e}")
    finally:
        # 恢复原始配置
        settings.QWEN_API_KEY = original_api_key

async def test_different_models():
    """测试不同的Qwen模型"""
    print("\n=== 测试不同Qwen模型 ===")
    
    if not settings.QWEN_API_KEY:
        print("❌ 需要QWEN_API_KEY才能测试不同模型")
        return
    
    models = ["qwen-turbo", "qwen-plus", "qwen-max"]
    original_model = settings.LLM_MODEL
    
    for model in models:
        print(f"\n--- 测试模型: {model} ---")
        settings.LLM_MODEL = model
        
        try:
            provider = LLMProvider()
            llm = provider.get_llm()
            
            question = "请用一句话介绍人工智能。"
            print(f"问题: {question}")
            
            response = llm.invoke(question)
            print(f"回答: {response}")
            print(f"✅ {model} 测试成功")
            
        except Exception as e:
            print(f"❌ {model} 测试失败: {e}")
    
    # 恢复原始模型配置
    settings.LLM_MODEL = original_model

async def main():
    """主测试函数"""
    print("开始Qwen LLM集成测试...")
    print(f"当前配置: LLM_MODEL={settings.LLM_MODEL}")
    print(f"API密钥设置: {'已设置' if settings.QWEN_API_KEY else '未设置'}")
    
    await test_qwen()
    await test_mock()
    await test_different_models()
    
    print("\n=== 测试完成 ===")
    print("说明:")
    print("- 需要在.env文件中设置QWEN_API_KEY")
    print("- 支持的模型: qwen-turbo, qwen-plus, qwen-max")
    print("- qwen-turbo: 响应最快，成本最低")
    print("- qwen-plus: 平衡版本，推荐使用")
    print("- qwen-max: 效果最好，成本最高")

if __name__ == "__main__":
    asyncio.run(main()) 