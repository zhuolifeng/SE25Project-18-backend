#!/usr/bin/env python3
"""
系统测试脚本 - 测试所有组件是否正常工作
"""

import requests
import json
import time
import sys
from typing import Dict, Any

class SystemTester:
    def __init__(self):
        self.python_rag_url = "http://localhost:8002"
        self.java_backend_url = "http://localhost:8080"
        self.qdrant_url = "http://localhost:6333"
        
    def test_python_rag_service(self) -> bool:
        """测试Python RAG服务"""
        print("🔍 测试Python RAG服务...")
        try:
            # 测试健康检查
            response = requests.get(f"{self.python_rag_url}/api/health", timeout=5)
            if response.status_code == 200:
                print("   ✅ Python RAG服务健康检查通过")
                print(f"   响应: {response.json()}")
                return True
            else:
                print(f"   ❌ Python RAG服务健康检查失败: {response.status_code}")
                return False
        except requests.exceptions.RequestException as e:
            print(f"   ❌ Python RAG服务连接失败: {e}")
            return False
    
    def test_qdrant_vector_db(self) -> bool:
        """测试Qdrant向量数据库"""
        print("\n🔍 测试Qdrant向量数据库...")
        try:
            # 测试Qdrant API
            response = requests.get(f"{self.qdrant_url}/collections", timeout=5)
            if response.status_code == 200:
                print("   ✅ Qdrant向量数据库连接成功")
                collections = response.json()
                print(f"   集合数量: {len(collections.get('collections', []))}")
                return True
            else:
                print(f"   ❌ Qdrant连接失败: {response.status_code}")
                return False
        except requests.exceptions.RequestException as e:
            print(f"   ❌ Qdrant连接失败: {e}")
            return False
    
    def test_java_backend(self) -> bool:
        """测试Java后端服务"""
        print("\n🔍 测试Java后端服务...")
        try:
            # 测试Java后端健康检查
            response = requests.get(f"{self.java_backend_url}/api/users/current", timeout=5)
            if response.status_code == 200:
                print("   ✅ Java后端服务运行正常")
                return True
            elif response.status_code == 401:
                print("   ⚠️  Java后端服务运行正常（需要认证）")
                return True
            else:
                print(f"   ❌ Java后端服务异常: {response.status_code}")
                return False
        except requests.exceptions.RequestException as e:
            print(f"   ❌ Java后端服务连接失败: {e}")
            return False
    
    def test_chat_functionality(self) -> bool:
        """测试聊天功能"""
        print("\n🔍 测试聊天功能...")
        try:
            # 测试聊天查询
            chat_data = {
                "message": "你好，请简单介绍一下人工智能",
                "user_id": "test_user"
            }
            
            response = requests.post(
                f"{self.python_rag_url}/api/chat/query",
                json=chat_data,
                timeout=30
            )
            
            if response.status_code == 200:
                result = response.json()
                print("   ✅ 聊天功能测试成功")
                print(f"   问题: {chat_data['message']}")
                print(f"   回答: {result.get('message', result.get('answer', ''))[:100]}...")
                print(f"   来源数量: {len(result.get('sources', []))}")
                return True
            else:
                print(f"   ❌ 聊天功能测试失败: {response.status_code}")
                print(f"   错误信息: {response.text}")
                return False
                
        except requests.exceptions.RequestException as e:
            print(f"   ❌ 聊天功能连接失败: {e}")
            return False
    
    def test_document_processing(self) -> bool:
        """测试文档处理功能"""
        print("\n🔍 测试文档处理功能...")
        try:
            # 测试文档处理状态
            response = requests.get(f"{self.python_rag_url}/api/documents/status", timeout=5)
            if response.status_code == 200:
                result = response.json()
                print("   ✅ 文档处理服务正常")
                print(f"   状态: {result.get('message', '')}")
                return True
            else:
                print(f"   ❌ 文档处理服务异常: {response.status_code}")
                return False
                
        except requests.exceptions.RequestException as e:
            print(f"   ❌ 文档处理服务连接失败: {e}")
            return False
    
    def test_qwen_integration(self) -> bool:
        """测试Qwen API集成"""
        print("\n🔍 测试Qwen API集成...")
        try:
            # 测试简单的问答
            test_data = {
                "message": "请用一句话回答：什么是机器学习？",
                "user_id": "test_user"
            }
            
            response = requests.post(
                f"{self.python_rag_url}/api/chat/query",
                json=test_data,
                timeout=30
            )
            
            if response.status_code == 200:
                result = response.json()
                answer = result.get('message', result.get('answer', ''))
                if answer and len(answer) > 10:
                    print("   ✅ Qwen API集成成功")
                    print(f"   回答: {answer[:100]}...")
                    return True
                else:
                    print("   ❌ Qwen API返回空回答")
                    return False
            else:
                print(f"   ❌ Qwen API测试失败: {response.status_code}")
                return False
                
        except requests.exceptions.RequestException as e:
            print(f"   ❌ Qwen API连接失败: {e}")
            return False
    
    def run_all_tests(self):
        """运行所有测试"""
        print("🚀 开始系统测试...")
        print("=" * 50)
        
        tests = [
            ("Python RAG服务", self.test_python_rag_service),
            ("Qdrant向量数据库", self.test_qdrant_vector_db),
            ("Java后端服务", self.test_java_backend),
            ("文档处理功能", self.test_document_processing),
            ("Qwen API集成", self.test_qwen_integration),
            ("聊天功能", self.test_chat_functionality),
        ]
        
        results = []
        for test_name, test_func in tests:
            try:
                result = test_func()
                results.append((test_name, result))
            except Exception as e:
                print(f"   ❌ {test_name}测试异常: {e}")
                results.append((test_name, False))
        
        # 输出测试结果总结
        print("\n" + "=" * 50)
        print("📊 测试结果总结:")
        print("=" * 50)
        
        passed = 0
        total = len(results)
        
        for test_name, result in results:
            status = "✅ 通过" if result else "❌ 失败"
            print(f"{test_name:<20} {status}")
            if result:
                passed += 1
        
        print("=" * 50)
        print(f"总计: {passed}/{total} 项测试通过")
        
        if passed == total:
            print("🎉 所有测试通过！系统运行正常")
            return True
        else:
            print("⚠️  部分测试失败，请检查相关服务")
            return False

def main():
    """主函数"""
    tester = SystemTester()
    success = tester.run_all_tests()
    
    if success:
        print("\n🎯 系统测试完成，可以开始使用聊天机器人功能！")
        print("\n📝 使用示例:")
        print("1. 前端调用: POST http://localhost:8080/api/chat/query")
        print("2. 直接调用: POST http://localhost:8002/api/chat/query")
        print("3. 查看文档: http://localhost:8002/docs")
    else:
        print("\n🔧 请检查失败的服务并重新运行测试")
        sys.exit(1)

if __name__ == "__main__":
    main() 