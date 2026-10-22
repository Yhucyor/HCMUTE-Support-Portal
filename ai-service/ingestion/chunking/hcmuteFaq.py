import json
import logging
from pathlib import Path
from datetime import datetime
from ingestion.helpers.make_metadata import make_metadata

logger = logging.getLogger("ingestion")

def chunk_hcmute_faqs():
    # Tìm tệp dataset FAQ
    possible_paths = [
        Path("data/processed/faq_dataset_curated.json"),
        Path("../docs/dataset/faq_dataset_curated.json"),
        Path("docs/dataset/faq_dataset_curated.json")
    ]
    
    file_path = None
    for p in possible_paths:
        if p.exists():
            file_path = p
            break
            
    if not file_path:
        logger.error("Không tìm thấy tệp dataset FAQ tại các đường dẫn quy định.")
        return []
        
    logger.info(f"Đang đọc dữ liệu FAQ từ {file_path}...")
    try:
        with open(file_path, "r", encoding="utf-8") as f:
            faqs = json.load(f)
    except Exception as e:
        logger.error(f"Lỗi khi đọc file FAQ: {e}")
        return []

    chunks = []
    for item in faqs:
        if not isinstance(item, dict):
            continue
        
        faq_id = str(item.get("id", ""))
        title = item.get("title", "")
        question = item.get("question", "")
        answer = item.get("answer", "")
        
        if not question and not title:
            continue
            
        q_text = question if question else title
        a_text = answer if answer else "Vui lòng liên hệ phòng ban chuyên trách để được hỗ trợ trực tiếp."
        
        text_content = f"Hỏi đáp học vụ HCMUTE\nTiêu đề: {title}\nCâu hỏi: {q_text}\nGiải đáp: {a_text}"
        
        base_metadata = {
            "type": "hcmute_faq",
            "item_id": faq_id,
            "title": title[:100],
            "source": "faq_dataset_curated.json",
            "created_at": datetime.utcnow().isoformat(),
            "language": "vi"
        }
        
        chunks.append({
            "text": text_content,
            "metadata": make_metadata(base_metadata, chunk_type="faq_chunk", priority=1)
        })
        
    logger.info(f"Đã tạo {len(chunks)} chunks từ kho câu hỏi FAQ HCMUTE.")
    return chunks
