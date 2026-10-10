import json
import logging
from pathlib import Path
from datetime import datetime

from core.settings_loader import load_settings
from ingestion.helpers.make_metadata import make_metadata

settings = load_settings()
logger = logging.getLogger("ingestion")

def chunk_interior_styles():
    file_path = Path(settings["data"]["processed_dir"]) / "interiorStyles.json"
    
    if not file_path.exists():
        logger.error(f"File not found: {file_path}")
        return []
    
    try: 
        with open(file_path, 'r', encoding='utf-8') as file:
            interior_styles = json.load(file)
    except json.JSONDecodeError as e:
        logger.error(f"Invalid JSON format {e}")
        return []
    except Exception as e:
        logger.error(f"Failed to load {e}")
        return []
    
    # Chuyen tu dict sang list neu can thiet
    if isinstance(interior_styles, dict):
        interior_styles = [interior_styles]
        
    # kiem tra xem co phai list khong
    if not isinstance(interior_styles, list):
        logger.error("Interior styles data is not a list")
        return []
    
    if not interior_styles:
        logger.warning("No interior styles found in the file")
        return []
    
    chunks = []
    
    for idx, interior_style in enumerate(interior_styles):
        if not isinstance(interior_style, dict):
            logger.warning(f"Interior style at index {idx} is not a dictionary")
            continue
        
        interior_id = interior_style.get("id")
        interior_slug = interior_style.get("slug", "")
        interior_name = interior_style.get("name", "")
        interior_image_url = interior_style.get("imageUrl", "")

        base_metadata = {
            "type": "interior_style",
            "interior_id": interior_id,
            "interior_name": interior_name,
            "interior_slug": interior_slug,
            "source": "interiorStyles.json",
            "created_at": datetime.utcnow().isoformat(),
            "language": "vi"
        }
        
        if interior_name and interior_image_url:
            text_parts = []
            text_parts.append(f"Tên phong cách nội thất: {interior_name}.")
            text_parts.append(f"URL hình ảnh minh họa phong cách nội thất: {interior_image_url}.")
            text = "\n".join(text_parts)
            chunks.append({
                "text": text,
                "metadata": make_metadata(
                    base_metadata, 
                    chunk_type="definition", 
                    priority=3)
            })

    return chunks