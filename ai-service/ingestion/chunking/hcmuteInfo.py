import json
import logging
from pathlib import Path
from datetime import datetime

from core.settings_loader import load_settings
from ingestion.helpers.make_metadata import make_metadata

settings = load_settings()
logger = logging.getLogger("ingestion")

def chunk_hcmute_info():
    file_path = Path(settings["data"]["processed_dir"]) / "hcmute_data.json"
    
    if not file_path.exists():
        logger.error(f"File not found: {file_path}")
        return []
    
    try:
        with open(file_path, "r", encoding="utf-8") as f:
            hcmute_info = json.load(f)
    except Exception as e:
        logger.error(f"Failed to load {e}")
        return []
    
    if isinstance(hcmute_info, dict):
        hcmute_info = [hcmute_info]
        
    if not isinstance(hcmute_info, list):
        return []
    
    chunks = []
    
    for item in hcmute_info:
        if not isinstance(item, dict):
            continue
            
        item_id = item.get("id", "")
        title = item.get("title", "")
        content = item.get("content", "")
        category = item.get("category", "General")
        
        base_metadata = {
            "type": "hcmute_info",
            "item_id": item_id,
            "category": category,
            "source": "hcmute_data.json",
            "created_at": datetime.utcnow().isoformat(),
            "language": "vi"
        }
        
        if title and content:
            chunks.append({
                "text": f"Chủ đề: {title}\nNội dung: {content}",
                "metadata": make_metadata(base_metadata, chunk_type="info_chunk", priority=1)
            })
            
    return chunks
