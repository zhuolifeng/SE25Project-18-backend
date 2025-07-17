from fastapi import FastAPI
from pydantic import BaseModel
from typing import List
from fastapi.middleware.cors import CORSMiddleware
from . import recommender as rec

app = FastAPI(title="User‑centric Paper Recommender")

# 允许的前端地址
origins = [
    "http://localhost:3000",      # React/Vite dev 服务器
    "http://127.0.0.1:3000",
    # 生产环境可再加正式域名
]

app.add_middleware(
    CORSMiddleware,
    allow_origins=origins,        # 精确或 ["*"]（不推荐线上）
    allow_credentials=True,
    allow_methods=["GET", "POST", "OPTIONS"],
    allow_headers=["*"],
)

class Hit(BaseModel):
    paper_id: int
    score: float

@app.get("/recommend/{user_id}", response_model=List[Hit])
def recommend(user_id: int, k: int = 10):
    """
    根据用户过去的收藏 / 浏览 / 点赞，生成 Top‑K 论文推荐。
    """
    return rec.recommend_for_user(user_id, k)
