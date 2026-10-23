import logging
from ingestion.chunking.hcmuteInfo import chunk_hcmute_info
from ingestion.chunking.hcmuteFaq import chunk_hcmute_faqs
from ingestion.chunking.hcmutePdf import chunk_hcmute_pdfs
from core.logging_setup import setup_logging
from vectorstore.upsert import upsert_chunks

setup_logging()
logger = logging.getLogger("ingestion")

def run_ingestion_pipeline():
    all_chunks = []
    
    # 1. Nạp dữ liệu HCMUTE cơ bản và kho 349 câu hỏi FAQ
    logger.info("Đang nạp dữ liệu giới thiệu và FAQ...")
    all_chunks.extend(chunk_hcmute_info())
    all_chunks.extend(chunk_hcmute_faqs())
    
    # 2. Nạp toàn bộ 91 tài liệu PDF công văn HCMUTE
    logger.info("Đang nạp và cắt đoạn 91 file PDF công văn...")
    all_chunks.extend(chunk_hcmute_pdfs())
    
    if not all_chunks:
        logger.warning("No chunks to upsert.")
        return
    
    logger.info(f"Tổng số chunks cần đưa vào Qdrant Cloud: {len(all_chunks)}. Bắt đầu embedding và upsert...")
    upsert_chunks(all_chunks, recreate=True)
    logger.info(f"Đã hoàn thành nạp {len(all_chunks)} chunks vào Qdrant Cloud!")
    
if __name__ == "__main__":
    run_ingestion_pipeline()