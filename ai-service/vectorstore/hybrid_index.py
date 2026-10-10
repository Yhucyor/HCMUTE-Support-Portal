import logging
import uuid

from qdrant_client.models import PointStruct, SparseVector
from embedding.embedder import embed_texts
from embedding.sparse_embedder import SparseEmbedder

logger = logging.getLogger("vector_database")

_sparse_embedder: SparseEmbedder | None = None

def init_sparse_embedder(embedder: SparseEmbedder):
    global _sparse_embedder
    _sparse_embedder = embedder # khoi tao sparse embedder cho hybrid index
    logger.info("Sparse embedder initialized for hybrid index.")
    
def build_hybrid_qdrant_points(chunks: list[dict]) -> list[dict]:
    if not chunks:
        logger.warning("No chunks provided to build hybrid Qdrant points.")
        return []
    
    if _sparse_embedder is None:
        raise RuntimeError("Sparse embedder not initialized. Call init_sparse_embedder first.")
    
    texts = [chunk["text"] for chunk in chunks]
    if not texts:
        logger.warning("No text found in the provided chunks.")
        return []
    
    dense_embeddings = embed_texts(texts)
    sparse_embeddings = _sparse_embedder.encode_batch(texts)
    
    points = []
    
    for chunk, dense_vector, sparse_vector in zip(chunks, dense_embeddings, sparse_embeddings):
        point = PointStruct(
            id=chunk.get("metadata", {}).get("chunk_id", str(uuid.uuid4())),
            vector={
                "dense": dense_vector.tolist() if hasattr(dense_vector, 'tolist') else dense_vector,
                "sparse": SparseVector(
                    indices=sparse_vector["indices"],
                    values=sparse_vector["values"]
                ),
            },
            payload={
                "text": chunk["text"], 
                **chunk.get("metadata", {})
            }
        )
        points.append(point)
        
    logger.info(f"Built {len(points)} hybrid Qdrant points.")
    return points