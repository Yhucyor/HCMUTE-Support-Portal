import json
import logging
from pathlib import Path
from datetime import datetime

from core.settings_loader import load_settings
from ingestion.helpers.make_metadata import make_metadata
from ingestion.helpers.split_paragraphs import split_paragraphs

settings = load_settings()
logger = logging.getLogger("ingestion")

def chunk_projects():
    file_path = Path(settings["data"]["processed_dir"]) / "projects.json"
    
    if not file_path.exists():
        logger.error(f"File not found: {file_path}")
        return []
    
    try:
        with open(file_path, "r", encoding="utf-8") as f:
            projects = json.load(f)
    except json.JSONDecodeError as e:
        logger.error(f"Invalid JSON format {e}")
        return []
    except Exception as e:
        logger.error(f"Failed to load {e}")
        return []   
    
    if isinstance(projects, dict):
        projects = [projects]
        
    if not isinstance(projects, list):
        logger.error("Projects data is not a list")
        return []
    
    if not projects:
        logger.warning("No projects found in the file")
        return []
    
    chunks = []
    
    for idx, project in enumerate(projects):
        if not isinstance(project, dict):
            logger.warning(f"Project at index {idx} is not a dict")
            continue
        
        project_id = project.get("id")
        project_name = project.get("title", "").strip()
        project_slug = project.get("slug", "")
        project_investor = project.get("investor", "")
        project_location = project.get("location", "")
        project_description = project.get("description", "")
        project_thumbnail_url = project.get("thumbnailUrl", "")
        project_completed_date = project.get("completedDate", "")
        project_area = project.get("area") if isinstance(project.get("area"), (int, float)) else None
        category = project.get("category", {})
        interior = project.get("interiorStyle", {})
       
        base_metadata = {
            "type": "project",
            "project_id": project_id,
            "project_name": project_name,
            "project_slug": project_slug,
            "source": "projects.json",
            "created_at": datetime.utcnow().isoformat(),
            "language": "vi",
        }
        
        CHUNK_PRIORITY = {
            "overview": 1,
            "description": 2,
            "style": 3,
            "context": 4,
            "specs": 5,
            "media": 6
        }
        
        # 1/ Overview chunk
        if project_name:
            chunks.append({
                "text": f"Dự án {project_name}.",
                "metadata": make_metadata(base_metadata, chunk_type="overview", priority=CHUNK_PRIORITY["overview"])
            })
        
        # 2/ Description chunk
        for i, part in enumerate(split_paragraphs(project_description)):
            chunks.append({
                "text": f"Mô tả dự án {project_name}: {part}",
                "metadata": make_metadata(
                    base_metadata,
                    chunk_type="description",
                    priority=CHUNK_PRIORITY["description"],
                    part_index=i
                )
            })

        # 3/ Category + Interior style chunk
        category = category or {}
        interior = interior or {}
        text_parts = []
        if category.get("name"):
            text_parts.append(f"Danh mục: {category['name']}")
        if interior.get("name"):
            text_parts.append(f"Phong cách nội thất: {interior['name']}")

        chunks.append({
            "text": f"Dự án {project_name}. " + ". ".join(text_parts),
            "metadata": make_metadata(
                base_metadata,
                chunk_type="style",
                project_category_name=category.get("name"),
                project_interior_name=interior.get("name"),
                priority=CHUNK_PRIORITY["style"]
            )
        })

        # 4/ Location + Investor chunk
        if project_location or project_investor:
            text_parts = []
            if project_location:
                text_parts.append(f"Địa điểm: {project_location}")
            if project_investor:
                text_parts.append(f"Chủ đầu tư: {project_investor}")

            chunks.append({
                "text": f"Dự án {project_name}. " + ". ".join(text_parts),
                "metadata": make_metadata(
                    base_metadata,
                    chunk_type="context",
                    project_location=project_location,
                    project_investor=project_investor,
                    priority=CHUNK_PRIORITY["context"]
                )
            })

        # 5/ Specs chunk (area, time)
        if project_area or project_completed_date:
            text_parts = []
            if project_area:
                text_parts.append(f"Diện tích {project_area} m²")
            if project_completed_date:
                text_parts.append(f"Hoàn thành năm {project_completed_date[:4]}")

            chunks.append({
                "text": f"Dự án {project_name}. " + ", ".join(text_parts),
                "metadata": make_metadata(
                    base_metadata,
                    chunk_type="specs",
                    project_area=project_area,
                    project_completed_date=project_completed_date,
                    priority=CHUNK_PRIORITY["specs"]
                )
            })

        # 6/ Media chunk
        if project_thumbnail_url:
            chunks.append({
                "text": f"Dự án {project_name} có hình ảnh minh họa.",
                "metadata": make_metadata(
                    base_metadata,
                    chunk_type="media",
                    project_image_url=project_thumbnail_url,
                    priority=CHUNK_PRIORITY["media"]
                )
            })
            
    return chunks