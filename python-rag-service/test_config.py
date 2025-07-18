#!/usr/bin/env python3
"""
配置测试脚本
用于测试配置是否正确加载
"""

import os
import sys
import logging

# 添加项目根目录到Python路径
root_path = os.path.dirname(os.path.abspath(__file__))
if root_path not in sys.path:
    sys.path.append(root_path)

# 导入配置
from config import config, configure_logging

# 配置日志
configure_logging()
logger = logging.getLogger("config_test")

def test_config_loading():
    """测试配置加载"""
    logger.info("=== 配置加载测试 ===")
    
    # 服务配置
    logger.info(f"SERVICE_HOST: {config.SERVICE_HOST}")
    logger.info(f"SERVICE_PORT: {config.SERVICE_PORT}")
    logger.info(f"DEBUG_MODE: {config.DEBUG_MODE}")
    
    # 日志配置
    logger.info(f"LOG_LEVEL: {config.LOG_LEVEL}")
    
    # CORS 配置
    logger.info(f"ALLOWED_ORIGINS: {config.ALLOWED_ORIGINS}")
    
    # Qwen API 配置
    masked_api_key = "***" if config.QWEN_API_KEY else "未设置"
    logger.info(f"QWEN_API_KEY: {masked_api_key}")
    
    # 向量数据库配置
    logger.info(f"VECTOR_STORE_TYPE: {config.VECTOR_STORE_TYPE}")
    
    # Qdrant 配置
    logger.info(f"QDRANT_HOST: {config.QDRANT_HOST}")
    logger.info(f"QDRANT_PORT: {config.QDRANT_PORT}")
    logger.info(f"QDRANT_COLLECTION_NAME: {config.QDRANT_COLLECTION_NAME}")
    
    # LLM 设置
    logger.info(f"LLM_PROVIDER: {config.LLM_PROVIDER}")
    logger.info(f"LLM_MODEL: {config.LLM_MODEL}")
    
    # RAG 设置
    logger.info(f"RAG_CHUNK_SIZE: {config.RAG_CHUNK_SIZE}")
    logger.info(f"RAG_CHUNK_OVERLAP: {config.RAG_CHUNK_OVERLAP}")
    logger.info(f"RAG_TOP_K: {config.RAG_TOP_K}")
    
    logger.info("=== 配置加载测试完成 ===")
    return True

def test_env_variables():
    """测试环境变量覆盖默认配置"""
    logger.info("=== 环境变量测试 ===")
    
    # 显示原始值
    logger.info(f"当前服务端口: {config.SERVICE_PORT}")
    
    # 设置环境变量
    test_port = "8003"
    os.environ["SERVICE_PORT"] = test_port
    logger.info(f"设置环境变量 SERVICE_PORT={test_port}")
    
    # 重新导入配置
    logger.info("重新加载配置...")
    try:
        import importlib
        import config as cfg_module
        importlib.reload(cfg_module)
        from config import config as new_config
        
        # 检查新值
        logger.info(f"重新加载后的服务端口: {new_config.SERVICE_PORT}")
        
        # 恢复环境变量
        del os.environ["SERVICE_PORT"]
        logger.info("环境变量测试完成")
        return True
    except Exception as e:
        logger.error(f"环境变量测试失败: {str(e)}")
        return False

def main():
    """主函数"""
    success = True
    
    # 测试配置加载
    if not test_config_loading():
        logger.error("配置加载测试失败")
        success = False
    
    # 测试环境变量
    if not test_env_variables():
        logger.error("环境变量测试失败")
        success = False
    
    if success:
        logger.info("所有测试通过!")
        sys.exit(0)
    else:
        logger.error("测试失败!")
        sys.exit(1)

if __name__ == "__main__":
    main() 