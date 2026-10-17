import logging
from qdrant_client import QdrantClient

from core.settings_loader import load_settings
from vectorstore.qdrant import get_qdrant_client, ensure_collection
from vectorstore.hybrid_index import build_hybrid_qdrant_points, init_sparse_embedder
from embedding.sparse_embedder import SparseEmbedder

settings = load_settings()
logger = logging.getLogger("vector_database")

QDRANT_CONFIG = settings["vector_database"]
COLLECTION_NAME = QDRANT_CONFIG["collection_name"]

def upsert_chunks(chunks: list[dict]):
    if not chunks:
        logger.warning("No chunks provided to build Qdrant points.")
        return []
    
    client: QdrantClient = get_qdrant_client()
    ensure_collection(client)
    
    # Fit sparse embedder with corpus
    logger.info("Fitting sparse embedder with corpus...")
    texts = [chunk["text"] for chunk in chunks]
    sparse_embedder = SparseEmbedder()
    sparse_embedder.fit(texts)
    init_sparse_embedder(sparse_embedder)
    logger.info(f"Sparse embedder fitted with vocabulary size: {len(sparse_embedder.vocabulary)}")
    
    # Build hybrid points
    points = build_hybrid_qdrant_points(chunks)
    
    if not points:
        logger.warning("No points were built from the provided chunks.")
        return []
    
    client.upsert(collection_name=COLLECTION_NAME, points=points)
    logger.info(f"Upserted {len(points)} hybrid points into collection '{COLLECTION_NAME}'.")