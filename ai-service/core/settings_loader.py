import os
import yaml
from pathlib import Path

def load_settings():
    """Load settings from YAML and override with environment variables"""
    with open("config/settings.yaml") as file:
        settings = yaml.safe_load(file)
    
    # Override với environment variables
    if os.getenv("APP_ENV"):
        settings["app"]["env"] = os.getenv("APP_ENV")
    
    # Vector database overrides
    if os.getenv("QDRANT_URL"):
        settings["vector_database"]["url"] = os.getenv("QDRANT_URL")
    if os.getenv("QDRANT_API_KEY"):
        settings["vector_database"]["api_key"] = os.getenv("QDRANT_API_KEY")
    if os.getenv("QDRANT_COLLECTION_NAME"):
        settings["vector_database"]["collection_name"] = os.getenv("QDRANT_COLLECTION_NAME")
    if os.getenv("QDRANT_TIMEOUT"):
        settings["vector_database"]["timeout"] = int(os.getenv("QDRANT_TIMEOUT"))
    
    # Embedding overrides
    if os.getenv("EMBEDDING_MODEL"):
        settings["embedding"]["model"] = os.getenv("EMBEDDING_MODEL")
    if os.getenv("EMBEDDING_DEVICE"):
        settings["embedding"]["device"] = os.getenv("EMBEDDING_DEVICE")
    if os.getenv("EMBEDDING_BATCH_SIZE"):
        settings["embedding"]["batch_size"] = int(os.getenv("EMBEDDING_BATCH_SIZE"))
    
    # LLM overrides
    if os.getenv("LLM_PROVIDER"):
        settings["llm"]["provider"] = os.getenv("LLM_PROVIDER")
    if os.getenv("LLM_MODEL_NAME"):
        settings["llm"]["model_name"] = os.getenv("LLM_MODEL_NAME")
    if os.getenv("LLM_BASE_URL"):
        settings["llm"]["base_url"] = os.getenv("LLM_BASE_URL")
    if os.getenv("LLM_TEMPERATURE"):
        settings["llm"]["temperature"] = float(os.getenv("LLM_TEMPERATURE"))
    if os.getenv("LLM_MAX_TOKENS"):
        settings["llm"]["max_tokens"] = int(os.getenv("LLM_MAX_TOKENS"))
    if os.getenv("LLM_TIMEOUT"):
        settings["llm"]["timeout"] = int(os.getenv("LLM_TIMEOUT"))
    
    # Retrieval overrides
    if os.getenv("RETRIEVAL_TOP_K"):
        settings["retrieval"]["top_k"] = int(os.getenv("RETRIEVAL_TOP_K"))
    if os.getenv("RETRIEVAL_SCORE_THRESHOLD"):
        settings["retrieval"]["score_threshold"] = float(os.getenv("RETRIEVAL_SCORE_THRESHOLD"))
    if os.getenv("DENSE_WEIGHT"):
        settings["retrieval"]["dense_weight"] = float(os.getenv("DENSE_WEIGHT"))
    if os.getenv("BM25_WEIGHT"):
        settings["retrieval"]["bm25_weight"] = float(os.getenv("BM25_WEIGHT"))
    
    # Reranking overrides
    if "reranking" not in settings:
        settings["reranking"] = {}
    if os.getenv("RERANKING_MODEL"):
        settings["reranking"]["model"] = os.getenv("RERANKING_MODEL")
    if os.getenv("RERANKING_DEVICE"):
        settings["reranking"]["device"] = os.getenv("RERANKING_DEVICE")
    if os.getenv("RERANKING_TOP_K"):
        settings["reranking"]["top_k"] = int(os.getenv("RERANKING_TOP_K"))
    
    return settings