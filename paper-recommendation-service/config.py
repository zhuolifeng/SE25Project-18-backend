from pathlib import Path
import os
from dotenv import load_dotenv

load_dotenv()

BASE_DIR = Path(__file__).resolve().parent

# ---------- DB ----------
DB_USER = os.getenv("DB_USER")
DB_PASS = os.getenv("DB_PASS")
DB_HOST = os.getenv("DB_HOST")
DB_PORT = os.getenv("DB_PORT")
DB_NAME = os.getenv("DB_NAME")

DATABASE_URL = (
    f"mysql+pymysql://{DB_USER}:{DB_PASS}@{DB_HOST}:{DB_PORT}/{DB_NAME}?charset=utf8mb4"
)

# ---------- Model ----------
MODEL_NAME = os.getenv("MODEL_NAME", "allenai-specter")
EMBED_DIM = 768 if "specter" in MODEL_NAME else 384

# ---------- Paths ----------
INDEX_PATH = BASE_DIR / "faiss.index"
EMB_MATRIX_PATH = BASE_DIR / "embeddings.npy"   # N × dim
IDMAP_PATH      = BASE_DIR / "paper_ids.npy"    # 长度 N，对应每行 paper_id

# ---------- SQL ----------
BASE_QUERY = """
SELECT
    id AS paper_id,
    title,
    abstract_text
FROM papers
WHERE has_abstract = TRUE
"""
