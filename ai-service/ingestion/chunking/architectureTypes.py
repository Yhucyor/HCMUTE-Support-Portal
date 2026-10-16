import json
import logging
from pathlib import Path

from datetime import datetime

from core.settings_loader import load_settings
from ingestion.helpers.make_metadata import make_metadata

settings = load_settings()
logger = logging.getLogger("ingestion")

def chunk_architecture_types():
    # Khai bao file path
    file_path = Path(settings["data"]["processed_dir"]) / "architectureTypes.json"
    
    # Kiem tra file ton tai
    if not file_path.exists():
        logger.error(f"File not found: {file_path}")
        return []
    
    # Doc du lieu tu file JSON,
    try:
        with open(file_path, "r", encoding="utf-8") as f:
            architecture_types = json.load(f)
    # neu file json khong hop le thi log loi
    except json.JSONDecodeError as e: # bat loi khi doc file json
        logger.error(f"Invalid JSON format {e}")
        return []
    # neu khong co du lieu thi log loi
    except Exception as e:
        logger.error(f"Failed to load {e}")
        return []
    
    if isinstance(architecture_types, dict): # neu du lieu la dict thi chuyen thanh list chua dict. Vi du: { "name": "Modern" } => [ { "name": "Modern" } ]
        architecture_types = [architecture_types]
    
    if not isinstance(architecture_types, list): # kiem tra kieu du lieu co phai la list khong. Vi du: [ { "name": "Modern" }, { "name": "Contemporary" } ]
        logger.error("Architecture types data is not a list")
        return []

    if not architecture_types: # kiem tra danh sach co rong khong. VI du: []
        logger.warning("No architecture types found in the data")
        return []
    
    chunks = [] # danh sach luu cac doan van ban
    
    for idx, architecture_type in enumerate(architecture_types):
        if not isinstance(architecture_type, dict): # kiem tra kieu du lieu co phai la dict khong. Vi du: { "name": "Modern" }
            logger.warning(f"Invalid architecture type at index {idx}: expected a dictionary")
            continue
        
        architecture_id = architecture_type.get("id")
        architecture_slug = architecture_type.get("slug", "")
        architecture_name = architecture_type.get("name", "")
        architecture_description = architecture_type.get("description", "")
        
        base_metadata = {
            "type": "architecture_type",
            "architecture_type_id": architecture_id,
            "architecture_type_name": architecture_name,
            "architecture_type_slug": architecture_slug,
            "source": "architectureTypes.json",
            "created_at": datetime.utcnow().isoformat(),
            "language": "vi"
        }
        
        if architecture_name and architecture_description:
            text_parts = []
            text_parts.append(f"Tên phong cách kiến trúc: {architecture_name}.")
            text_parts.append(f"Mô tả phong cách kiến trúc: {architecture_description}.")
            text = "\n".join(text_parts)
            chunks.append({
                "text": text,
                "metadata": make_metadata(
                    base_metadata, 
                    chunk_type="definition", 
                    priority=3)
            })
            
    return chunks