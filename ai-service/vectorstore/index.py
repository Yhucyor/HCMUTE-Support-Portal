import logging
import uuid

from embedding.embedder import embed_texts

logger = logging.getLogger("embedding")

def build_qdrant_points(chunks: list[dict]) -> list[dict]: # list[dict] truyen vo la list cac chunk va list[dict] tra ve la list cac point
    if not chunks:
        logger.warning("No chunks provided to build Qdrant points.")
        return []
    
    texts = [chunk["text"] for chunk in chunks]
    if not texts:
        logger.warning("No text found in the provided chunks.")
        return []
    
    embeddings = embed_texts(texts)
    if not embeddings:
        logger.warning("No embeddings generated for the provided texts.")
        return []
    
    points = []
    
    for chunk, vector in zip(chunks, embeddings): # zip de lay tung chunk va vector tuong ung
        points.append({
            "id": chunk.get("metadata", {}).get("chunk_id", str(uuid.uuid4())),
            "vector": vector, # embedding vector
            "payload": {
                "text": chunk["text"], 
                **chunk.get("metadata", {}) # them metadata neu co
            }
        })
        
    logger.info(f"Built {len(points)} Qdrant points.")
    return points