import json
import logging
from pathlib import Path
from datetime import datetime

from core.settings_loader import load_settings
from ingestion.helpers.make_metadata import make_metadata

settings = load_settings()
logger = logging.getLogger("ingestion")

def chunk_project_categories():
    file_path = Path(settings["data"]["processed_dir"]) / "projectCategories.json"
    
    if not file_path.exists():
        logger.error(f"File not found: {file_path}")
        return []
    
    try:
        with open(file_path, "r", encoding="utf-8") as f:
            project_categories = json.load(f)
    except json.JSONDecodeError as e:
        logger.error(f"Invalid JSON format {e}")
        return []
    except Exception as e:
        logger.error(f"Failed to load {e}")
        return []
    
    if isinstance(project_categories, dict):
        project_categories = [project_categories]
        
    if not isinstance(project_categories, list):
        logger.error("Project categories data is not a list")
        return []
    
    if not project_categories:
        logger.warning("No project categories found in the file")
        return []
    
    chunks = []
    
    for idx, category in enumerate(project_categories):
        if not isinstance(category, dict):
            logger.warning(f"Category at index {idx} is not a dict")
            continue
        
        project_category_id = category.get("id")
        project_category_name = category.get("name", "")
        project_category_slug = category.get("slug", "")
        
        if not project_category_name:
            logger.warning(f"Project category at index {idx} has invalid or missing name")
            continue
        
        base_metadata = {
            "type": "project_category",
            "project_category_id": project_category_id,
            "project_category_name": project_category_name,
            "project_category_slug": project_category_slug,
            "source": "projectCategories.json",
            "created_at": datetime.utcnow().isoformat(),
            "language": "vi",
        }
        
        text = (
            f"Tên danh mục dự án: {project_category_name}"
            f"Danh mục dự án này được dùng để phân loại các dự án liên quan đến {project_category_name}."
        )
        
        chunks.append({
            "text": text,
            "metadata": make_metadata(
                base_metadata,
                chunk_type="definition",
                priority=3
            )
        })
        
    return chunks