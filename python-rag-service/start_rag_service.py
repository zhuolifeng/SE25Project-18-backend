#!/usr/bin/env python3
"""
RAG服务启动脚本
提供更好的启动、停止和状态监控功能
"""

# 首先检查依赖
import subprocess
import sys
import os

# 检查是否需要依赖检查
check_deps = True
for arg in sys.argv:
    if arg == "--skip-deps-check":
        check_deps = False
        break

# 运行依赖检查脚本
if check_deps:
    print("正在检查依赖...")
    check_script = os.path.join(os.path.dirname(__file__), "check_and_install_dependencies.py")
    
    if os.path.exists(check_script):
        result = subprocess.call([sys.executable, check_script])
        if result != 0:
            print("依赖检查失败，退出启动")
            sys.exit(1)
    else:
        print(f"警告: 依赖检查脚本不存在: {check_script}")

# 正常启动逻辑
import argparse
import logging
import os
import sys
import time
import signal
import requests
import uvicorn
from contextlib import contextmanager

# 添加项目根目录到Python路径
root_path = os.path.dirname(os.path.abspath(__file__))
if root_path not in sys.path:
    sys.path.append(root_path)

# 导入配置
from config import config, configure_logging

# 配置日志
configure_logging()
logger = logging.getLogger("rag_service_launcher")

def parse_arguments():
    """Parse command line arguments"""
    parser = argparse.ArgumentParser(description='Start the RAG service')
    parser.add_argument('--host', help='Host to run the service on')
    parser.add_argument('--port', type=int, help='Port to run the service on')
    parser.add_argument('--debug', action='store_true', help='Run in debug mode')
    parser.add_argument('--reload', action='store_true', help='Enable auto-reload')
    parser.add_argument('--log-level', choices=['DEBUG', 'INFO', 'WARNING', 'ERROR', 'CRITICAL'],
                        default='INFO', help='Set the logging level')
    return parser.parse_args()

@contextmanager
def handle_keyboard_interrupt():
    """Handle keyboard interrupt gracefully"""
    try:
        yield
    except KeyboardInterrupt:
        print("\nService shutdown requested by user.")
        print("Stopping server...")
        time.sleep(1)
        print("Server stopped.")

def start_service(host=None, port=None, debug=None, reload=None, log_level=None):
    """Start the RAG service"""
    from config import Config
    
    # Use provided parameters or defaults from config
    host = host or Config.SERVICE_HOST
    port = port or Config.SERVICE_PORT
    debug_mode = debug or Config.DEBUG_MODE
    reload_mode = reload if reload is not None else debug_mode  # 如果未指定reload，则使用debug值
    
    # Setup environment variable for log level
    if log_level:
        os.environ["LOG_LEVEL"] = log_level
    
    print(f"Starting RAG service on {host}:{port}")
    print(f"Debug mode: {debug_mode}")
    print(f"Auto-reload: {reload_mode}")
    print(f"Log level: {log_level or os.environ.get('LOG_LEVEL', 'INFO')}")

    try:
        uvicorn.run(
            "app.main:app",
            host=host,
            port=port,
            log_level=(log_level or "info").lower(),
            reload=reload_mode
            # 移除了不支持的debug参数
        )
    except Exception as e:
        print(f"Error starting server: {e}")
        sys.exit(1)

def check_status():
    """Check if the service is already running"""
    from config import Config
    
    host = Config.SERVICE_HOST
    port = Config.SERVICE_PORT
    
    url = f"http://{host}:{port}/api/health"
    
    try:
        response = requests.get(url, timeout=2)
        if response.status_code == 200:
            print(f"RAG service is already running at {host}:{port}")
            print(f"Status: {response.json()}")
            print("\nYou can use this running instance or stop it and start a new one.")
            print("To stop the service, find its process and terminate it.")
            
            # 添加快捷启动调试端点命令
            print("\n调试命令:")
            print(f"  检查PDF提取状态: curl http://{host}:{port}/api/documents/pdf-status/{{paper_id}}")
            print(f"  检查向量存储: curl http://{host}:{port}/api/documents/debug-vector/{{paper_id}}")
            return True
    except requests.exceptions.ConnectionError:
        # Service is not running or not reachable
        return False
    except Exception as e:
        print(f"Error checking service status: {e}")
        return False

def main():
    """Main entry point"""
    args = parse_arguments()
    
    # 配置日志
    try:
        # 先尝试配置基本的控制台日志，确保至少能看到错误
        log_level = getattr(logging, args.log_level, logging.INFO) if args.log_level else logging.INFO
        log_format = '%(asctime)s - %(name)s - %(levelname)s - %(message)s'
        logging.basicConfig(level=log_level, format=log_format)
        
        # 显示配置信息
        print("\n=== RAG服务启动 ===")
        print(f"日志级别: {args.log_level}")
        print(f"日志文件: {os.path.abspath('rag_service_debug.log')}")
        
        # 设置环境变量，以便app/main.py也能读取
        if args.log_level:
            os.environ["LOG_LEVEL"] = args.log_level
    except Exception as e:
        print(f"配置日志时出错: {str(e)}")
        print("使用默认日志配置继续")
    
    if not check_status():
        print("Starting new RAG service instance...")
        try:
            with handle_keyboard_interrupt():
                start_service(
                    host=args.host, 
                    port=args.port, 
                    debug=args.debug, 
                    reload=args.reload,
                    log_level=args.log_level
                )
        except Exception as e:
            logging.critical(f"启动服务时遇到严重错误: {str(e)}")
            import traceback
            logging.critical(traceback.format_exc())
            print("\n发生错误! 查看日志以获取更多信息。")
            sys.exit(1)
    else:
        print("To start a new instance, make sure to stop the current one first.")

if __name__ == "__main__":
    main() 