from qdrant_client import QdrantClient
from qdrant_client.models import VectorParams, Distance, SparseVectorParams, SparseIndexParams
import logging 
import os

from core.settings_loader import load_settings

settings = load_settings()
logger = logging.getLogger("vector_database")

QDRANT_CONFIG = settings["vector_database"]
COLLECTION_NAME = QDRANT_CONFIG["collection_name"] # khong dung .get vi day la bat buoc phai co
VECTOR_SIZE = QDRANT_CONFIG["vector_size"] # khong dung .get vi day la bat buoc phai co
DISTANCE = QDRANT_CONFIG.get("distance", "cosine") # mac dinh la cosine, co the la "dot" hoac "euclid"
TIMEOUT = QDRANT_CONFIG.get("timeout", 30) # Default 30 seconds

_client: QdrantClient | None = None

def get_qdrant_client() -> QdrantClient: # them -> QdrantClient de tra ve
    global _client
    if _client is not None:
        return _client
    
    try:
        # Neu ket noi qua URL
        if QDRANT_CONFIG.get("url"):
            logger.info("Connect via URL")
            _client = QdrantClient(
                url=QDRANT_CONFIG["url"],
                api_key=QDRANT_CONFIG.get("api_key"),
                timeout=TIMEOUT
            )
        elif QDRANT_CONFIG.get("path"):
            logger.info(f"Connect via Local Database (No Docker): {QDRANT_CONFIG.get('path')}")
            _client = QdrantClient(path=QDRANT_CONFIG.get("path"))
        else:
            logger.info(f"Connect via: {QDRANT_CONFIG.get('host')}: {QDRANT_CONFIG.get('port')}")
            
            _client = QdrantClient(
                host=QDRANT_CONFIG.get("host"),
                port=QDRANT_CONFIG.get("port"),
                api_key=QDRANT_CONFIG.get("api_key"),
                timeout=TIMEOUT
            )
        
        # Test connection: use get_collection on target collection to support scoped Cloud API keys
        target_col = COLLECTION_NAME or QDRANT_CONFIG.get("collection_name", "hcmute_chatbot_collection")
        try:
            _client.get_collection(target_col)
        except Exception as e:
            logger.debug(f"Target collection check note: {e}")
        logger.info("Successfully connected to Qdrant")
        return _client
    
    except Exception as e:
        logger.error(f"Failed to connect to Qdrant: {e}")
        raise ConnectionError(f"Cannot connect to Qdrant database: {e}")

def ensure_collection(client: QdrantClient, recreate: bool = False):
    try:
        if not recreate and client.collection_exists(COLLECTION_NAME):
            logger.info(f"Collection '{COLLECTION_NAME}' already exists.")
            return
    except Exception:
        pass
    
    if recreate:
        try:
            if client.collection_exists(COLLECTION_NAME):
                logger.info(f"Recreating collection '{COLLECTION_NAME}'...")
                client.delete_collection(COLLECTION_NAME)
        except Exception:
            pass
    
    logger.info(f"Creating collection '{COLLECTION_NAME}' with hybrid vectors (dense + sparse)...")
    client.create_collection(
        collection_name=COLLECTION_NAME,
        vectors_config={
            "dense": VectorParams(
                size=VECTOR_SIZE,
                distance=Distance[DISTANCE.upper()]
            )
        },
        sparse_vectors_config={
            "sparse": SparseVectorParams(
                index=SparseIndexParams()
            )
        }
    )
    logger.info(f"Collection '{COLLECTION_NAME}' created with dense vector size {VECTOR_SIZE}, distance '{DISTANCE}', and sparse vectors.")
    