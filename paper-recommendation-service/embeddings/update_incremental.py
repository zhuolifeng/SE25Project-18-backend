"""
增量更新：把数据库中新插入的论文向量追加进索引。
"""
import sqlalchemy as sa
import pandas as pd
import numpy as np
from sentence_transformers import SentenceTransformer
from tqdm import tqdm
import faiss
from pathlib import Path
import sys

ROOT = Path(__file__).resolve().parents[1]
sys.path.append(str(ROOT))
from config import DATABASE_URL, BASE_QUERY, MODEL_NAME, EMBED_DIM, INDEX_PATH

BATCH = 128


def load_index():
    if not INDEX_PATH.exists():
        raise FileNotFoundError("faiss.index not found, run build_index.py first")
    return faiss.read_index(str(INDEX_PATH))


def fetch_new_rows(engine, existing_ids):
    sql = f"""
    SELECT id AS paper_id, title, abstract_text
    FROM papers
    WHERE has_abstract = TRUE
      AND id NOT IN ({','.join(map(str, existing_ids)) or 0})
    """
    return pd.read_sql(sql, engine)


def encode(texts, model):
    vecs = np.empty((len(texts), EMBED_DIM), dtype="float32")
    for i in tqdm(range(0, len(texts), BATCH), desc="Encoding"):
        vecs[i : i + BATCH] = model.encode(
            texts[i : i + BATCH], batch_size=BATCH, show_progress_bar=False
        )
    faiss.normalize_L2(vecs)
    return vecs


def main():
    index = load_index()
    existing_ids = set(index.id_map.keys())
    engine = sa.create_engine(DATABASE_URL)
    df = fetch_new_rows(engine, existing_ids)

    if df.empty:
        print("No new papers 🎉")
        return

    model = SentenceTransformer(MODEL_NAME)
    vecs = encode((df["title"] + " [SEP] " + df["abstract_text"]).tolist(), model)

    index.add_with_ids(vecs, df["paper_id"].values.astype("int64"))
    faiss.write_index(index, str(INDEX_PATH))
    print(f"✅ added {len(df):,} new papers, total={index.ntotal:,}")


if __name__ == "__main__":
    main()
