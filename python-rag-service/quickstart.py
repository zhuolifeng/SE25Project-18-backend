"""
Quick Start Example - How to integrate Python RAG with Java Backend
This demonstrates the integration between services
"""

import requests
import json

# Configuration
JAVA_BACKEND_URL = "http://localhost:8080"
PYTHON_RAG_URL = "http://localhost:8002"

def demo_integration():
    """Demonstrate the integration flow"""
    
    print("=== Python RAG Service Integration Demo ===\n")
    
    # Step 1: Check if both services are running
    print("1. Checking services...")
    try:
        java_health = requests.get(f"{JAVA_BACKEND_URL}/api/users/current")
        print(f"   ✓ Java Backend is running")
    except:
        print(f"   ✗ Java Backend is not running at {JAVA_BACKEND_URL}")
        print("   Please start the Java backend first")
        return
    
    try:
        rag_health = requests.get(f"{PYTHON_RAG_URL}/api/health")
        print(f"   ✓ Python RAG Service is running")
    except:
        print(f"   ✗ Python RAG Service is not running at {PYTHON_RAG_URL}")
        print("   Please start the Python RAG service with: python -m app.main")
        return
    
    # Step 2: Process some papers
    print("\n2. Processing papers into vector database...")
    paper_ids = ["paper001", "paper002", "paper003"]  # These should exist in your Java backend
    
    response = requests.post(
        f"{PYTHON_RAG_URL}/api/documents/process",
        json={"paper_ids": paper_ids}
    )
    
    if response.status_code == 200:
        result = response.json()
        print(f"   ✓ Processed {result['processed']} papers successfully")
    else:
        print(f"   ✗ Failed to process papers: {response.text}")
        return
    
    # Step 3: Test chatbot query
    print("\n3. Testing chatbot query...")
    query = "What are the main applications of machine learning in NLP?"
    
    response = requests.post(
        f"{PYTHON_RAG_URL}/api/chat/query",
        json={
            "question": query,
            "user_id": "demo_user"
        }
    )
    
    if response.status_code == 200:
        result = response.json()
        print(f"   Question: {query}")
        print(f"   Answer: {result['answer'][:200]}...")
        print(f"   Sources: {len(result['sources'])} papers found")
    else:
        print(f"   ✗ Failed to query: {response.text}")
    
    print("\n=== Demo Complete ===")

if __name__ == "__main__":
    demo_integration() 