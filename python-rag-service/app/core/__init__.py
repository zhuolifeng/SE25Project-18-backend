"""
Core module init
此文件使app.core成为一个完整的Python包
并提供直接导入核心组件的便利
"""

import sys
import os

# 添加项目根目录到Python路径
root_path = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", ".."))
if root_path not in sys.path:
    sys.path.insert(0, root_path)

# 版本信息
__version__ = "1.0.0"

# 预加载模块
try:
    from app.core.vector_store import VectorStoreManager
    from app.core.llm_provider import LLMProvider
    from config import config, configure_logging
    
    # 配置初始日志级别
    configure_logging()
    
except ImportError as e:
    print(f"初始化app.core时出现导入错误: {str(e)}") 