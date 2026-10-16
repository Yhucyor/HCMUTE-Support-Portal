import logging
from typing import Optional

from qdrant_client import QdrantClient
from embedding.sparse_embedder import SparseEmbedder
from scoring.bm25 import BM25
from reranking.reranker import CrossEncoderReranker
from reranking.models.cross_encoder import CrossEncoderModel
from vectorstore.qdrant import get_qdrant_client
from vectorstore.hybrid_index import init_sparse_embedder
from core.settings_loader import load_settings

settings = load_settings()
logger = logging.getLogger("startup")

COLLECTION_NAME = settings["vector_database"]["collection_name"]
RERANKER_CONFIG = settings.get("reranking", {})
RERANKER_MODEL = RERANKER_CONFIG.get("model", "cross-encoder/ms-marco-MiniLM-L-6-v2")
RERANKER_DEVICE = RERANKER_CONFIG.get("device", "cpu")

_sparse_embedder: Optional[SparseEmbedder] = None
_bm25: Optional[BM25] = None
_reranker: Optional[CrossEncoderReranker] = None
_initialized = False

def initialize_rag_components():
    """
    Initialize all RAG components at startup:
    1. Load corpus from Qdrant
    2. Fit SparseEmbedder
    3. Initialize BM25
    4. Initialize Reranker
    """
    global _sparse_embedder, _bm25, _reranker, _initialized
    
    if _initialized:
        logger.info("RAG components already initialized")
        return {
            "sparse_embedder": _sparse_embedder,
            "bm25": _bm25,
            "reranker": _reranker
        }
    
    try:
        # Step 1: Load corpus from Qdrant
        logger.info("Step 1: Loading corpus from Qdrant...")
        client: QdrantClient = get_qdrant_client()
        
        try:
            # Scroll through all documents in collection
            documents_texts = []
            offset = None
            batch_size = 100
            
            while True:
                result = client.scroll(
                    collection_name=COLLECTION_NAME,
                    limit=batch_size,
                    offset=offset,
                    with_payload=True,
                    with_vectors=False  # Don't need vectors, only texts
                )
                
                points, next_offset = result
                
                if not points:
                    break
                
                for point in points:
                    text = point.payload.get("text", "")
                    if text:
                        documents_texts.append(text)
                
                if next_offset is None:
                    break
                    
                offset = next_offset
            
            if not documents_texts:
                logger.warning("No documents found in Qdrant collection. Components will not be initialized.")
                return None
            
            logger.info(f"Loaded {len(documents_texts)} documents from corpus")
        
        except Exception as e:
            logger.error(f"Failed to load corpus from Qdrant: {e}")
            logger.warning("Continuing without BM25 and reranker initialization")
            return None
        
        # Step 2: Initialize SparseEmbedder
        logger.info("Step 2: Fitting SparseEmbedder with corpus...")
        _sparse_embedder = SparseEmbedder()
        _sparse_embedder.fit(documents_texts)
        init_sparse_embedder(_sparse_embedder)
        logger.info(f"SparseEmbedder fitted with vocabulary size: {len(_sparse_embedder.vocabulary)}")
        
        # Step 3: Initialize BM25
        logger.info("Step 3: Initializing BM25 scorer...")
        _bm25 = BM25(_sparse_embedder, k1=1.5, b=0.75)
        _bm25.compute_average_document_length(documents_texts)
        logger.info(f"BM25 initialized with avg doc length: {_bm25.average_document_length:.2f}")
        
        # Step 4: Initialize Reranker
        logger.info("Step 4: Initializing CrossEncoder Reranker...")
        cross_encoder = CrossEncoderModel(RERANKER_MODEL, device=RERANKER_DEVICE)
        _reranker = CrossEncoderReranker(cross_encoder)
        logger.info(f"Reranker initialized with model: {RERANKER_MODEL}")
        
        _initialized = True
        
        logger.info("RAG components initialization completed successfully!")
        
        return {
            "sparse_embedder": _sparse_embedder,
            "bm25": _bm25,
            "reranker": _reranker
        }
        
    except Exception as e:
        logger.error(f"Failed to initialize RAG components: {e}", exc_info=True)
        logger.warning("Application will continue with basic retrieval only")
        return None

def get_bm25() -> Optional[BM25]:
    return _bm25

def get_reranker() -> Optional[CrossEncoderReranker]:
    return _reranker

def get_initialization_status() -> dict:
    return {
        "initialized": _initialized,
        "sparse_embedder": _sparse_embedder is not None,
        "bm25": _bm25 is not None,
        "reranker": _reranker is not None,
        "vocabulary_size": len(_sparse_embedder.vocabulary) if _sparse_embedder else 0,
        "avg_doc_length": _bm25.average_document_length if _bm25 else 0.0
    }
