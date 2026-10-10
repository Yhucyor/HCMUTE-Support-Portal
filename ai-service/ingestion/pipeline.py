import logging
from ingestion.chunking.hcmuteInfo import chunk_hcmute_info
from core.logging_setup import setup_logging
from vectorstore.upsert import upsert_chunks

setup_logging()
logger = logging.getLogger("ingestion")

def run_ingestion_pipeline():
    all_chunks = []
    
    # Nạp dữ liệu HCMUTE
    all_chunks.extend(chunk_hcmute_info())
    
    if not all_chunks:
        logger.warning("No chunks to upsert.")
        return
    
    upsert_chunks(all_chunks)
    logger.info(f"Upserted {len(all_chunks)} chunks into the vector store.")
    
if __name__ == "__main__":
    run_ingestion_pipeline()