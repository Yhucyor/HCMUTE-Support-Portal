import logging
import re
from pathlib import Path
from datetime import datetime
from pypdf import PdfReader
from ingestion.helpers.make_metadata import make_metadata

logger = logging.getLogger("ingestion")

def clean_text(text: str) -> str:
    if not text:
        return ""
    # Normalize multiple whitespace and newlines
    text = re.sub(r"[ \t]+", " ", text)
    text = re.sub(r"\n\s*\n+", "\n\n", text)
    return text.strip()

def split_into_windows(text: str, chunk_size: int = 500, overlap: int = 60) -> list[str]:
    chunks = []
    start = 0
    text_len = len(text)
    while start < text_len:
        end = min(start + chunk_size, text_len)
        # Try to break at a period, newline or space near end
        if end < text_len:
            break_pt = text.rfind(". ", start, end)
            if break_pt == -1:
                break_pt = text.rfind("\n", start, end)
            if break_pt == -1:
                break_pt = text.rfind(" ", start, end)
            if break_pt > start + 100:
                end = break_pt + 1
        
        chunk = text[start:end].strip()
        if len(chunk) > 40:
            chunks.append(chunk)
        start = end - overlap if end < text_len else text_len
    return chunks

def chunk_hcmute_pdfs(tailieu_dir: str = r"C:\Users\LENOVO\Downloads\tailieuAI") -> list[dict]:
    p = Path(tailieu_dir)
    if not p.exists():
        logger.warning(f"Thư mục tài liệu không tồn tại: {tailieu_dir}")
        return []
    
    pdf_files = sorted(list(p.rglob("*.pdf")))
    logger.info(f"Tìm thấy {len(pdf_files)} file PDF tài liệu HCMUTE tại {tailieu_dir}...")
    
    all_chunks = []
    
    for pdf_path in pdf_files:
        rel_folder = pdf_path.parent.name
        doc_name = pdf_path.name
        doc_title = pdf_path.stem
        # Clean title prefix numbers like '1. ', '02. '
        doc_title_clean = re.sub(r"^\d+[\.\-_]\s*", "", doc_title)
        
        try:
            reader = PdfReader(str(pdf_path))
            total_pages = len(reader.pages)
            extracted_any = False
            
            for page_idx, page in enumerate(reader.pages):
                page_num = page_idx + 1
                try:
                    page_text = clean_text(page.extract_text() or "")
                except Exception:
                    page_text = ""
                
                if len(page_text) >= 50:
                    extracted_any = True
                    text_windows = split_into_windows(page_text, chunk_size=500, overlap=60)
                    for win_idx, win in enumerate(text_windows):
                        header = f"Văn bản quy định HCMUTE: {doc_title_clean} (Năm {rel_folder}, Trang {page_num}/{total_pages})\n"
                        full_content = header + win
                        
                        meta = {
                            "type": "hcmute_official_doc",
                            "doc_name": doc_name,
                            "title": doc_title_clean[:120],
                            "year": rel_folder,
                            "page": page_num,
                            "total_pages": total_pages,
                            "chunk_index": win_idx,
                            "source": doc_name,
                            "created_at": datetime.utcnow().isoformat(),
                            "language": "vi"
                        }
                        all_chunks.append({
                            "text": full_content,
                            "metadata": make_metadata(meta, chunk_type="pdf_chunk", priority=2)
                        })
            
            # If document is a scanned image with no extracted text, preserve document metadata
            if not extracted_any:
                header = f"Văn bản thông báo HCMUTE: {doc_title_clean} (Tập tin: {doc_name}, Năm: {rel_folder})."
                content = f"{header}\nNội dung văn bản quy chế/thông báo chính thức của Trường Đại học Sư phạm Kỹ thuật TP.HCM năm {rel_folder}. Vui lòng tham khảo trực tiếp văn bản gốc hoặc liên hệ phòng ban chuyên trách."
                meta = {
                    "type": "hcmute_official_doc",
                    "doc_name": doc_name,
                    "title": doc_title_clean[:120],
                    "year": rel_folder,
                    "page": 1,
                    "total_pages": total_pages,
                    "chunk_index": 0,
                    "source": doc_name,
                    "created_at": datetime.utcnow().isoformat(),
                    "language": "vi"
                }
                all_chunks.append({
                    "text": content,
                    "metadata": make_metadata(meta, chunk_type="pdf_scanned_stub", priority=3)
                })
                
        except Exception as e:
            logger.error(f"Lỗi khi đọc file PDF {pdf_path.name}: {e}")
            continue

    logger.info(f"Hoàn thành trích xuất {len(pdf_files)} PDF: Tổng cộng {len(all_chunks)} chunks.")
    return all_chunks
