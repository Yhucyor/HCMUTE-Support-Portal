import json
import logging
from pathlib import Path
from datetime import datetime

from core.settings_loader import load_settings
from ingestion.helpers.make_metadata import make_metadata

settings = load_settings()
logger = logging.getLogger("ingestion")

def chunk_news_categories():
    file_path = Path(settings["data"]["processed_dir"]) / "newsCategories.json"
    
    if not file_path.exists():
        logger.error(f"File not found: {file_path}")
        return []
    
    try:
        with open(file_path, "r", encoding="utf-8") as f:
            news_categories = json.load(f)
    except json.JSONDecodeError as e:
        logger.error(f"Invalid JSON format {e}")
        return []
    except Exception as e:
        logger.error(f"Failed to load {e}")
        return []
    
    if isinstance(news_categories, dict):
        news_categories = [news_categories]
        
    if not isinstance(news_categories, list):
        logger.error("News categories data is not a list or dict")
        return []
    
    if not news_categories:
        logger.warning("No news categories found in the file")
        return []
    
    chunks = []
    
    for idx, category in enumerate(news_categories):
        if not isinstance(category, dict):
            logger.warning(f"Category at index {idx} is not a dict")
            continue
        
        news_category_id = category.get("id")
        news_category_name = category.get("name", "")
        news_category_slug = category.get("slug", "")
        
        if not news_category_name:
            logger.warning(f"News category at index {idx} has invalid or missing name")
            continue
        
        base_metadata = {
            "type": "news_category",
            "news_category_id": news_category_id,
            "news_category_name": news_category_name,
            "news_category_slug": news_category_slug, 
            "source": "newsCategories.json",
            "created_at": datetime.utcnow().isoformat(),
            "language": "vi",
        }
        
        text = (
            f"Tên danh mục tin tức: {news_category_name}"
            f"Danh mục tin tức này được sử dụng để phân loại các bài viết liên quan đến {news_category_name}."
        )
        
        chunks.append({
            "text": text,
            "metadata": make_metadata(
                base_metadata,
                chunk_type="definition",
                priority=3
            )
        })
    
    if not chunks:
        logger.warning("No valid news category chunks were created")
    
    return chunks