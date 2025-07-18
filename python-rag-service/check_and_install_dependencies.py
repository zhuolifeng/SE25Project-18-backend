#!/usr/bin/env python3
"""
依赖检查和安装脚本
检查必要的依赖是否已安装，如果没有则自动安装
"""

import sys
import subprocess
import importlib.util
import os

# 颜色输出
GREEN = "\033[92m"
YELLOW = "\033[93m"
RED = "\033[91m"
RESET = "\033[0m"
BOLD = "\033[1m"

# 核心依赖
CORE_DEPENDENCIES = [
    "fastapi", 
    "uvicorn", 
    "langchain",
    "langchain-community",
    "sentence-transformers",
    "qdrant-client",
]

# PDF处理依赖
PDF_DEPENDENCIES = [
    "PyPDF2",
    "pdfplumber"
]

def check_dependency(package_name):
    """检查依赖是否已安装"""
    spec = importlib.util.find_spec(package_name.replace("-", "_"))
    return spec is not None

def install_package(package_name):
    """安装指定的包"""
    print(f"{YELLOW}正在安装 {package_name}...{RESET}")
    try:
        subprocess.check_call([sys.executable, "-m", "pip", "install", package_name])
        return True
    except subprocess.CalledProcessError:
        return False

def main():
    """主函数"""
    print(f"{BOLD}===== RAG服务依赖检查 ====={RESET}")
    
    # 检查核心依赖
    missing_core = []
    for dep in CORE_DEPENDENCIES:
        if check_dependency(dep):
            print(f"{GREEN}✓ {dep} 已安装{RESET}")
        else:
            print(f"{RED}✗ {dep} 未安装{RESET}")
            missing_core.append(dep)
    
    # 检查PDF处理依赖
    pdf_support = False
    missing_pdf = []
    for dep in PDF_DEPENDENCIES:
        if check_dependency(dep):
            pdf_support = True
            print(f"{GREEN}✓ {dep} 已安装{RESET}")
        else:
            print(f"{YELLOW}✗ {dep} 未安装{RESET}")
            missing_pdf.append(dep)
    
    # 安装缺少的核心依赖
    if missing_core:
        print(f"\n{BOLD}需要安装以下核心依赖:{RESET}")
        for dep in missing_core:
            if install_package(dep):
                print(f"{GREEN}✓ {dep} 安装成功{RESET}")
            else:
                print(f"{RED}✗ {dep} 安装失败{RESET}")
                print(f"{RED}无法继续，请手动安装依赖{RESET}")
                return False
    
    # 安装PDF处理依赖
    if not pdf_support and missing_pdf:
        print(f"\n{BOLD}PDF处理功能需要以下依赖之一:{RESET}")
        for dep in missing_pdf:
            if install_package(dep):
                print(f"{GREEN}✓ {dep} 安装成功{RESET}")
                pdf_support = True
                break
    
    # 确认安装结果
    print(f"\n{BOLD}==== 安装结果 ===={RESET}")
    if missing_core:
        if all(check_dependency(dep) for dep in missing_core):
            print(f"{GREEN}所有核心依赖已成功安装{RESET}")
        else:
            print(f"{RED}部分核心依赖安装失败，服务可能无法正常运行{RESET}")
            return False
    else:
        print(f"{GREEN}所有核心依赖已正确安装{RESET}")
    
    if pdf_support:
        print(f"{GREEN}PDF处理功能已可用{RESET}")
    else:
        print(f"{YELLOW}警告: PDF处理功能不可用，无法从PDF中提取文本{RESET}")
    
    print(f"\n{BOLD}系统已准备就绪！{RESET}")
    return True

if __name__ == "__main__":
    success = main()
    if success:
        # 如果参数包含--run，则启动服务
        if len(sys.argv) > 1 and sys.argv[1] == "--run":
            print(f"\n{GREEN}{BOLD}正在启动RAG服务...{RESET}")
            os.system(f"{sys.executable} -m app.main")
        sys.exit(0)
    else:
        print(f"\n{RED}{BOLD}依赖安装失败，无法启动服务{RESET}")
        sys.exit(1) 