import logging
import math
from collections import Counter

from embedding.sparse_embedder import tokenize, SparseEmbedder

logger = logging.getLogger("scoring")

class BM25:
    def __init__(self, sparse_embedder: SparseEmbedder, k1: float = 1.5, b: float = 0.75):
        self.sparse_embedder = sparse_embedder
        self.k1 = k1 # k1 dùng để điều chỉnh tần số xuất hiện của từ trong văn bản
        self.b = b
        self.num_documents = sparse_embedder.num_documents
        self.average_document_length = None
        
    def compute_average_document_length(self, documents: list[str]):
        total_length = 0 # tổng số token trong tất cả các tài liệu
        valid_documents = 0 # số tài liệu hợp lệ (không rỗng)
        
        for document in documents:
            tokens = tokenize(document)
            if not tokens:
                logger.warning("Empty document encountered while computing average document length.")
                continue
            total_length += len(tokens) # cộng dồn độ dài tài liệu
            valid_documents += 1 # đếm số tài liệu hợp lệ (không rỗng)
            
        if valid_documents == 0:
            logger.error("No valid documents to compute average document length.")
            self.average_document_length = 0.0
            return
        
        self.average_document_length = total_length / max(valid_documents, 1) # tính độ dài trung bình của tài liệu
        logger.debug(f"Computed average document length: {self.average_document_length}")
        
    def score(self, query: str, document: str) -> float:
        if not query or not document:
            logger.warning("Empty query or document provided for scoring.")
            return 0.0
        
        if self.average_document_length is None:
            logger.error("Average document length not computed. Call compute_average_document_length first.")
            return 0.0
        
        query_terms = tokenize(query)
        document_terms = tokenize(document)
        document_length = len(document_terms)
        term_frequency_document = Counter(document_terms)
        score = 0.0
        
        for term in set(query_terms): # query token unique
            if term not in self.sparse_embedder.vocabulary:
                logger.debug(f"Term '{term}' not found in vocabulary during scoring. Skipping.")
                continue
            
            document_frequency = self.sparse_embedder.document_frequency.get(term, 0)
            if document_frequency == 0:
                logger.debug(f"Term '{term}' has zero document frequency. Skipping.")
                continue
            
            inverse_document_frequency = math.log((self.num_documents - document_frequency + 0.5) / (document_frequency + 0.5) + 1)
            
            term_frequency = term_frequency_document.get(term, 0)
            if term_frequency == 0:
                logger.debug(f"Term '{term}' not found in document. Skipping.")
                continue
            
            numerator = term_frequency * (self.k1 + 1)
            denominator = term_frequency + self.k1 * (1 - self.b + self.b * (document_length / self.average_document_length))
            score += inverse_document_frequency * (numerator / denominator)
            
        return score
    
    def score_batch(self, query: str, documents: list[str]) -> list[float]:
        return [self.score(query, document) for document in documents]