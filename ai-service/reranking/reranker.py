import logging

from core.schema import RetrievedDocument
from reranking.base import BaseReranker
from reranking.models.cross_encoder import CrossEncoderModel

logger = logging.getLogger("reranking")

class CrossEncoderReranker(BaseReranker):
    def __init__(self, model: CrossEncoderModel):
        self.model = model

    def rerank(self, query: str, documents: list[RetrievedDocument], top_k: int | None = None) -> list[RetrievedDocument]:
        if not documents:
            return []

        # 1. Tạo cặp (query, document)
        pairs = [(query, doc.text) for doc in documents]

        # 2. Batch scoring
        scores = self.model.score_batch(pairs)

        # 3. Gắn score vào metadata
        for doc, score in zip(documents, scores):
            doc.metadata["rerank_score"] = float(score)

        # 4. Sort lại
        documents.sort(key=lambda d: d.metadata["rerank_score"], reverse=True)

        # 5. Cắt top_k nếu cần
        if top_k is not None:
            documents = documents[:top_k]

        logger.info(f"Reranked {len(documents)} documents")
        return documents