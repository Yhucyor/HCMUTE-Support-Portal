import logging
import os
from fastapi import APIRouter
from vectorstore.qdrant import get_qdrant_client
from core.settings_loader import load_settings

logger = logging.getLogger("health")
router = APIRouter()

@router.get("/health")
async def health_check():
    health_status = {
        "status": "healthy",
        "services": {}
    }
    
    try:
        client = get_qdrant_client()
        collections = client.get_collections()
        health_status["services"]["qdrant"] = {
            "status": "up",
            "collections": len(collections.collections)
        }
    except Exception as e:
        logger.error(f"Qdrant health check failed: {e}")
        health_status["status"] = "unhealthy"
        health_status["services"]["qdrant"] = {
            "status": "down",
            "error": str(e)
        }
    
    try:
        from embedding.embedder import get_model
        model = get_model()
        health_status["services"]["embedding"] = {
            "status": "up",
            "model": model.__class__.__name__
        }
    except Exception as e:
        logger.error(f"Embedding model health check failed: {e}")
        health_status["status"] = "degraded"
        health_status["services"]["embedding"] = {
            "status": "down",
            "error": str(e)
        }
    
    settings = load_settings()
    llm_config = settings.get("llm", {})
    health_status["services"]["llm"] = {
        "provider": llm_config.get("provider", "unknown"),
        "model": llm_config.get("model_name", "unknown"),
        "status": "configured"
    }
    
    # Add RAG components status
    try:
        from core.startup import get_initialization_status
        rag_status = get_initialization_status()
        health_status["services"]["rag_components"] = {
            "initialized": rag_status["initialized"],
            "sparse_embedder": "ready" if rag_status["sparse_embedder"] else "not initialized",
            "bm25": "ready" if rag_status["bm25"] else "not initialized",
            "reranker": "ready" if rag_status["reranker"] else "not initialized",
            "vocabulary_size": rag_status["vocabulary_size"],
            "avg_document_length": round(rag_status["avg_doc_length"], 2)
        }
    except Exception as e:
        logger.error(f"Failed to get RAG components status: {e}")
        health_status["services"]["rag_components"] = {"status": "error", "error": str(e)}
    
    return health_status
