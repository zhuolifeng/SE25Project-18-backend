import faiss, numpy as np, pandas as pd, sqlalchemy as sa
from pathlib import Path
import sys

ROOT = Path(__file__).resolve().parents[1]
sys.path.append(str(ROOT))
from config import (
    INDEX_PATH, MODEL_NAME, EMBED_DIM, DATABASE_URL,
    EMB_MATRIX_PATH, IDMAP_PATH
)

from sentence_transformers import SentenceTransformer

# ---------------- 资源加载 ----------------
index  = faiss.read_index(str(INDEX_PATH))      # 相似度检索使用
model  = SentenceTransformer(MODEL_NAME)        # 仅在冷启动时可能用，不用于画像取向量
engine = sa.create_engine(DATABASE_URL)

# 加载矩阵到内存（N × dim）
EMB_MATRIX = np.load(EMB_MATRIX_PATH).astype("float32")          # 原始未归一化
PAPER_IDS  = np.load(IDMAP_PATH).astype("int64")                  # 对应行号
# 建映射：paper_id -> row
ID2ROW = {int(pid): i for i, pid in enumerate(PAPER_IDS)}

# 为余弦运算准备归一化副本
EMB_MATRIX_NORM = EMB_MATRIX.copy()
faiss.normalize_L2(EMB_MATRIX_NORM)

# ---------- 交互权重（暂不含 post_likes） ----------
TABLES = [
    ("user_favorites",    "collect_time", 1.0),
    ("user_view_history", "view_time",    0.3),
]
DECAY_DAYS = 30

# ---------- 用户画像 ----------
def _user_profile_vec(user_id: int):
    dfs = []
    with engine.connect() as con:
        for tbl, ts_col, base_w in TABLES:
            q = f"""
            SELECT paper_id,
                   TIMESTAMPDIFF(DAY, {ts_col}, NOW()) AS age,
                   {base_w} AS base_w
            FROM {tbl}
            WHERE user_id = :uid
              AND paper_id IS NOT NULL
            """
            df = pd.read_sql(sa.text(q), con, params=dict(uid=user_id))
            if df.empty:
                continue
            df["w"] = df["base_w"] * np.exp(-df["age"] / DECAY_DAYS)
            dfs.append(df[["paper_id", "w"]])

    if not dfs:
        return None, set()

    inter = pd.concat(dfs).groupby("paper_id")["w"].sum()

    # 过滤索引中不存在的论文（或未建向量）
    valid_rows = []
    valid_ws   = []
    for pid, w in inter.items():
        row = ID2ROW.get(int(pid))
        if row is None:
            continue
        valid_rows.append(row)
        valid_ws.append(w)

    if not valid_rows:
        return None, set()

    vecs    = EMB_MATRIX_NORM[valid_rows]          # shape: M × dim
    weights = np.array(valid_ws, dtype="float32").reshape(-1, 1)
    user_vec = (vecs * weights).sum(axis=0, keepdims=True)

    faiss.normalize_L2(user_vec)
    seen = {int(PAPER_IDS[r]) for r in valid_rows}
    return user_vec.astype("float32"), seen

# ---------- 推荐 ----------
def recommend_for_user(user_id: int, k: int = 10):
    res = _user_profile_vec(user_id)
    if res is None:
        return []  # 冷启动：可返回热门论文
    user_vec, seen = res

    # 在索引中查相似论文
    D, I = index.search(user_vec, k + len(seen) + 50)
    out = []
    for pid, score in zip(I[0], D[0]):
        if pid == -1 or pid in seen:
            continue
        out.append({"paper_id": int(pid), "score": float(score)})
        if len(out) == k:
            break
    return out
