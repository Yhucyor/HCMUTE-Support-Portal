import json
import logging
from pathlib import Path
from bs4 import BeautifulSoup
from datetime import datetime

from core.settings_loader import load_settings
from ingestion.helpers.make_metadata import make_metadata
from ingestion.helpers.split_paragraphs import split_paragraphs

settings = load_settings()
logger = logging.getLogger("ingestion")

def html_to_text(html: str) -> str:  # Hàm nhận vào chuỗi HTML và trả về text thuần (không có tag)
    soup = BeautifulSoup(html, "html.parser")  # Parse chuỗi HTML bằng parser mặc định của Python
    return soup.get_text(separator=" ", strip=True)  
    # Lấy toàn bộ text trong HTML
    # separator=" " : chèn dấu cách giữa các đoạn text để tránh dính chữ
    # strip=True     : loại bỏ khoảng trắng dư ở đầu và cuối chuỗi

def chunk_news():
    file_path = Path(settings["data"]["processed_dir"]) / "news.json"
    
    if not file_path.exists():
        logger.error(f"File not found: {file_path}")
        return []
    
    try:
        with open(file_path, "r", encoding="utf-8") as file:
            news = json.load(file)
    except json.JSONDecodeError as e:
        logger.error(f"Invalid JSON format {e}")
        return []
    except Exception as e:
        logger.error(f"Failed to load {e}")
        return []
    
    if isinstance(news, dict):
        news = [news]
        
    if not isinstance(news, list):
        logger.error("News categories data is not a list")
        return []
    
    if not news:
        logger.warning("No news found in the file")
        return []
    
    chunks = []
    
    for idx, news_item in enumerate(news):
        if not isinstance(news_item, dict):
            logger.warning(f"News item at index {idx} is not a dictionary")
            continue
        
        news_item_id = news_item.get("id")
        news_item_title = news_item.get("title", "")
        news_item_slug = news_item.get("slug", "")
        news_item_excerpt = news_item.get("excerpt", "")
        news_item_content = news_item.get("content", "")
        news_item_content = html_to_text(news_item_content)
        news_item_content_split = split_paragraphs(news_item_content)
        
        base_metadata = {
            "type": "news",
            "news_item_id": news_item_id,
            "news_item_title": news_item_title,
            "news_item_slug": news_item_slug,
            "source": "news.json",
            "created_at": datetime.utcnow().isoformat(),
            "language": "vi",
        }
        
        CHUNK_PRIORITY = {
            "overview": 1,
            "full_content": 2,
        }
        
        # Chunk overview
        if news_item_title and news_item_excerpt:
            chunks.append({
                "text": f'Tựa đề tin tức: {news_item_title}\nTóm tắt tin tức: {news_item_excerpt}',
                "metadata": make_metadata(base_metadata, chunk_type="overview", priority=CHUNK_PRIORITY["overview"])
            })
        
        # Chunk full content
        for i, part in enumerate(news_item_content_split):
            chunks.append({
                "text": f"Nội dung tin tức {news_item_title}: {part}",
                "metadata": make_metadata(
                    base_metadata, 
                    chunk_type="full_content", 
                    priority=CHUNK_PRIORITY["full_content"]),
                "part_index": i
            })
                
 
    return chunks