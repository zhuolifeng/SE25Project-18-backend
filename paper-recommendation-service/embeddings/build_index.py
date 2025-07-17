import sqlalchemy as sa, pandas as pd, numpy as np, faiss
from sentence_transformers import SentenceTransformer
from tqdm import tqdm
from pathlib import Path
import sys

ROOT = Path(__file__).resolve().parents[1]
sys.path.append(str(ROOT))
from config import (
    DATABASE_URL, BASE_QUERY, MODEL_NAME, EMBED_DIM,
    INDEX_PATH, EMB_MATRIX_PATH, IDMAP_PATH
)

BATCH = 128

def main():
    engine = sa.create_engine(DATABASE_URL)
    df = pd.read_sql(BASE_QUERY, engine)
    print(f"Fetched {len(df):,} papers")

    model = SentenceTransformer(MODEL_NAME)
    texts = (df["title"] + " [SEP] " + df["abstract_text"]).tolist()
    embs  = np.empty((len(texts), EMBED_DIM), dtype="float32")

    for i in tqdm(range(0, len(texts), BATCH), desc="Encoding"):
        embs[i:i+BATCH] = model.encode(
            texts[i:i+BATCH], batch_size=BATCH, show_progress_bar=False
        )

    # 保存原始向量矩阵（画像计算要用）
    np.save(EMB_MATRIX_PATH, embs)
    np.save(IDMAP_PATH, df["paper_id"].values.astype("int64"))

    # 构建索引
    faiss.normalize_L2(embs)
    base  = faiss.IndexFlatIP(EMBED_DIM)
    index = faiss.IndexIDMap(base)
    index.add_with_ids(embs, df["paper_id"].values.astype("int64"))
    faiss.write_index(index, str(INDEX_PATH))
    print(f"✅ wrote {index.ntotal:,} vectors → {INDEX_PATH}")
    print(f"✅ saved embedding matrix → {EMB_MATRIX_PATH}")
    print(f"✅ saved id map → {IDMAP_PATH}")

if __name__ == "__main__":
    main()
