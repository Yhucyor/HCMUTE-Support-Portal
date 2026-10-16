import logging

from qdrant_client import QdrantClient
from qdrant_client.models import ScoredPoint
from qdrant_client.http.exceptions import ResponseHandlingException

from core.settings_loader import load_settings
from core.schema import RetrievedDocument
from vectorstore.qdrant import get_qdrant_client
from embedding.embedder import embed_texts

settings = load_settings()
logger = logging.getLogger("retrieval")

RETRIEVAL_CONFIG = settings["retrieval"]
COLLECTION_NAME = settings["vector_database"]["collection_name"]
RETRIEVAL_TOP_K = RETRIEVAL_CONFIG.get("top_k", 5)
RETRIEVAL_SCORE_THRESHOLD = RETRIEVAL_CONFIG.get("score_threshold", 0.3)

def retrieve(query: str) -> list[RetrievedDocument]:
    if not query or not query.strip(): # Kiểm tra query rỗng
        logger.warning("Received empty query for retrieval.")
        return []
    
    try:
        client: QdrantClient = get_qdrant_client() 
        vectors = embed_texts([query]) # embedding query, dung [query] de tra ve list vi embed nhan list
        if not vectors:
            logger.error("Failed to generate embedding for the query.")
            return []
        
        query_vector = vectors[0] # dung [0] vi chi co 1 query
        
        response = client.query_points( # truy van points tu qdrant
            collection_name=COLLECTION_NAME,
            query=query_vector,
            limit=RETRIEVAL_TOP_K,
            with_payload=True, # lay ca payload noi chua text va metadata
            score_threshold=RETRIEVAL_SCORE_THRESHOLD,
        )
        
        # response chua cac points va trong cac points la cac ScoredPoint
        
        points: list[ScoredPoint] = response.points # chu thich response.points la list cac ScoredPoint
        documents: list[RetrievedDocument] = [] # chuan hoa ket qua tra ve theo schema RetrievedDocument. Thay vi document = []
        
        for point in points:
            payload = point.payload or {} # neu payload la None thi gan payload la dict rong

            documents.append(
                RetrievedDocument(
                    id=str(point.id),
                    score=point.score,
                    text=payload.get("text", ""),
                    metadata={key: value for key, value in payload.items() if key != "text"},
                )
            )
            
        logger.info(f"Retrieved {len(documents)} documents")
        return documents
    
    except ResponseHandlingException as e:
        logger.error(f"Qdrant connection error: {e}")
        raise ConnectionError("Cannot connect to vector database")
    except Exception as e:
        logger.error(f"Error during retrieval: {e}", exc_info=True)
        return []