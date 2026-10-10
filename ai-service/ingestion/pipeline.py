import logging

from ingestion.chunking.architectureTypes import chunk_architecture_types
from ingestion.chunking.companyInfo import chunk_company_info
from ingestion.chunking.InteriorStyles import chunk_interior_styles
from ingestion.chunking.newCategories import chunk_news_categories
from ingestion.chunking.news import chunk_news
from ingestion.chunking.projectCategories import chunk_project_categories
from ingestion.chunking.projects import chunk_projects
from core.logging_setup import setup_logging

from vectorstore.upsert import upsert_chunks

setup_logging()
logger = logging.getLogger("ingestion")

def run_ingestion_pipeline():
    all_chunks = []
    
    all_chunks.extend(chunk_architecture_types())
    all_chunks.extend(chunk_company_info())
    all_chunks.extend(chunk_interior_styles())
    all_chunks.extend(chunk_news_categories())
    all_chunks.extend(chunk_news())
    all_chunks.extend(chunk_project_categories())
    all_chunks.extend(chunk_projects())
    
    if not all_chunks:
        logger.warning("No chunks to upsert.")
        return
    
    upsert_chunks(all_chunks)
    logger.info(f"Upserted {len(all_chunks)} chunks into the vector store.")
    
if __name__ == "__main__":
    run_ingestion_pipeline()