import logging
from typing import List

from qdrant_client import QdrantClient
from qdrant_client.models import ScoredPoint
from qdrant_client.http.exceptions import ResponseHandlingException

from core.settings_loader import load_settings
from core.schema import RetrievedDocument
from vectorstore.qdrant import get_qdrant_client
from embedding.embedder import embed_texts
from scoring.bm25 import BM25

settings = load_settings()
logger = logging.getLogger("retrieval")

COLLECTION_NAME = settings["vector_database"]["collection_name"]
RETRIEVAL_CONFIG = settings["retrieval"]
TOP_K = RETRIEVAL_CONFIG.get("top_k", 10)
SCORE_THRESHOLD = RETRIEVAL_CONFIG.get("score_threshold", 0.0)
DENSE_WEIGHT = RETRIEVAL_CONFIG.get("dense_weight", 0.6)
BM25_WEIGHT = RETRIEVAL_CONFIG.get("bm25_weight", 0.4)

def hybrid_retrieve(query: str, bm25: BM25) -> List[RetrievedDocument]:
    if not query or not query.strip():
        logger.warning("Empty query received for hybrid retrieval.")
        return []

    try:
        client: QdrantClient = get_qdrant_client()
        dense_vectors = embed_texts([query])
        if not dense_vectors:
            logger.error("Failed to embed query.")
            return []

        query_vector = dense_vectors[0]

        response = client.query_points(
            collection_name=COLLECTION_NAME,
            query=query_vector,
            using="dense",  # specify named vector for hybrid search
            limit=TOP_K * 3,  # lấy dư để rerank
            with_payload=True,
            score_threshold=SCORE_THRESHOLD,
        )

        points: list[ScoredPoint] = response.points
        documents: list[RetrievedDocument] = []

        for point in points:
            payload = point.payload or {}
            text = payload.get("text", "")

            if not text:
                continue

            bm25_score = bm25.score(query, text)
            hybrid_score = (DENSE_WEIGHT * point.score + BM25_WEIGHT * bm25_score)

            documents.append(
                RetrievedDocument(
                    id=str(point.id),
                    score=hybrid_score,
                    text=text,
                    metadata={
                        **{k: v for k, v in payload.items() if k != "text"},
                        "dense_score": point.score,
                        "bm25_score": bm25_score,
                    },
                )
            )

        documents.sort(key=lambda d: d.score, reverse=True)
        return documents[:TOP_K]
    
    except ResponseHandlingException as e:
        logger.error(f"Qdrant connection error: {e}")
        raise ConnectionError("Cannot connect to vector database")
    except Exception as e:
        logger.error(f"Error during retrieval: {e}", exc_info=True)
        return []