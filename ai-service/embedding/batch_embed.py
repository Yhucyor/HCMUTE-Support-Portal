import logging

from core.settings_loader import load_settings
from embedding.embedder import embed_texts

settings = load_settings()
logger = logging.getLogger("embedding")

BATCH_SIZE = settings["embedding"].get("batch_size", 32)

def batch_embed_texts(texts: list[str]) -> list[list[float]]:
    if not texts:
        logger.warning("No texts provided for embedding.")
        return []
    
    all_embeddings = []
    
    for start in range(0 , len(texts), BATCH_SIZE): # lay text tu vi tri 0 den batch sau do + 1 start de lay batch tiep theo
        batch = texts[start : start + BATCH_SIZE] # lay tung batch start la 0, den batch size
        
        logger.info(f"Embedding batch {start // BATCH_SIZE + 1}: size {len(batch)}")
        
        batch_embeddings = embed_texts(batch) # goi ham embed_texts tu file embedder.py de lay embedding cho tung batch
        all_embeddings.extend(batch_embeddings) # them tung batch embedding vao all_embeddings
        logger.info(f"Completed embedding batch {start // BATCH_SIZE + 1}")
    
    return all_embeddings