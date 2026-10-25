import logging
import gc
import torch
from sentence_transformers import SentenceTransformer

from core.settings_loader import load_settings

settings = load_settings()
logger = logging.getLogger("embedding")

EMBEDDING_CONFIG = settings["embedding"]
EMBEDDING_MODEL = EMBEDDING_CONFIG["model"]

_model = None

def get_model() -> SentenceTransformer:
    global _model
    if _model is None:
        logger.info(f"Loading embedding model: {EMBEDDING_MODEL} (bfloat16 low memory mode)...")
        try:
            _model = SentenceTransformer(
                EMBEDDING_MODEL,
                device="cpu",
                model_kwargs={"torch_dtype": torch.bfloat16, "low_cpu_mem_usage": True}
            )
            logger.info("Loaded embedding model successfully in bfloat16 mode")
        except Exception as e:
            logger.warning(f"bfloat16 not supported, falling back to standard CPU: {e}")
            _model = SentenceTransformer(EMBEDDING_MODEL, device=EMBEDDING_CONFIG.get("device", "cpu"))
        gc.collect()
    return _model

def embed_texts(texts: list[str]) -> list[list[float]]:
    if not texts:
        logger.warning("No texts provided for embedding.")
        return []
    
    model = get_model()
    with torch.inference_mode():
        embeddings = model.encode(texts, normalize_embeddings=True, convert_to_tensor=False).tolist()
    logger.info(f"Completed embedding texts {len(texts)}.")
    return embeddings
    