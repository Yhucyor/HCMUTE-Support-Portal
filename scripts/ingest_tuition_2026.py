import os
import sys
import io
import time
import json
import uuid
import re
from pathlib import Path
from pypdf import PdfReader
from PIL import Image
import google.generativeai as genai
from dotenv import load_dotenv

# Set paths
workspace_root = Path(r"d:\Laptrinh-Java\HCMUTE-Student-Portal")
ai_service_dir = workspace_root / "ai-service"
sys.path.insert(0, str(ai_service_dir))
os.chdir(str(ai_service_dir))

if sys.platform == "win32":
    sys.stdout.reconfigure(encoding="utf-8")
    sys.stderr.reconfigure(encoding="utf-8")

load_dotenv(ai_service_dir / ".env")

GEMINI_API_KEY = os.getenv("GEMINI_API_KEY")
if not GEMINI_API_KEY:
    print("ERROR: GEMINI_API_KEY not found in ai-service/.env")
    sys.exit(1)

genai.configure(api_key=GEMINI_API_KEY)
vision_model = genai.GenerativeModel("gemini-2.5-flash")

PDF_DIR = Path(r"C:\Users\LENOVO\Downloads\tailieuAI\T10 2026")
OUT_OCR_DIR = ai_service_dir / "data" / "ocr_tuition_2026"
OUT_OCR_DIR.mkdir(parents=True, exist_ok=True)

def ocr_pdf_pages(pdf_path: Path) -> list[dict]:
    reader = PdfReader(str(pdf_path))
    total_pages = len(reader.pages)
    print(f"Bắt đầu OCR {total_pages} trang của file {pdf_path.name}...")
    
    extracted_pages = []
    
    for idx, page in enumerate(reader.pages):
        page_num = idx + 1
        cache_file = OUT_OCR_DIR / f"page_{page_num}.txt"
        
        if cache_file.exists():
            print(f"Trang {page_num}/{total_pages} đã có trong bộ nhớ đệm (Cache). Đọc lại...")
            with open(cache_file, "r", encoding="utf-8") as f:
                page_text = f.read()
            extracted_pages.append({
                "page": page_num,
                "text": page_text
            })
            continue
        
        print(f"Đang phân tích OCR trang {page_num}/{total_pages} bằng Gemini Vision...")
        if not page.images:
            print(f"Trang {page_num} không có ảnh, thử extract text thuần...")
            txt = page.extract_text() or ""
            extracted_pages.append({"page": page_num, "text": txt})
            continue
        
        img_bytes = page.images[0].data
        pil_img = Image.open(io.BytesIO(img_bytes))
        
        prompt = (
            "Bạn là chuyên gia trích xuất dữ liệu học vụ. "
            "Hãy đọc và sao chép nguyên văn toàn bộ nội dung văn bản tiếng Việt trong ảnh công văn này.\n"
            "- Đặc biệt chú ý: GIỮ NGUYÊN tất cả các bảng biểu học phí, cột số tiền (VNĐ), "
            "mức thu theo tín chỉ, mức thu theo học kỳ/năm học, các mã ngành/khối ngành, "
            "thời hạn đóng học phí và các quy định miễn giảm.\n"
            "- Trình bày lại các bảng biểu theo định dạng bảng Markdown rõ ràng, chuẩn xác.\n"
            "- Giữ nguyên số quyết định, ngày ban hành và tiêu đề."
        )
        
        # Retry with exponential backoff
        for attempt in range(3):
            try:
                resp = vision_model.generate_content([prompt, pil_img])
                page_text = resp.text.strip()
                with open(cache_file, "w", encoding="utf-8") as f:
                    f.write(page_text)
                print(f"✅ Trang {page_num}/{total_pages} hoàn thành ({len(page_text)} ký tự)")
                extracted_pages.append({
                    "page": page_num,
                    "text": page_text
                })
                time.sleep(1.5)  # Tránh rate limit
                break
            except Exception as e:
                print(f"⚠️ Lỗi thử lần {attempt+1} trang {page_num}: {e}")
                time.sleep(3)
        else:
            print(f"❌ Không thể OCR trang {page_num}")
            extracted_pages.append({"page": page_num, "text": ""})

    return extracted_pages

def build_chunks(extracted_pages: list[dict], pdf_name: str) -> list[dict]:
    chunks = []
    total_pages = len(extracted_pages)
    clean_title = "Quy định thu học phí năm học 2026-2027 (Số 2807/QĐ-ĐHCNKT)"
    
    for item in extracted_pages:
        page_num = item["page"]
        text = item["text"]
        if not text or len(text.strip()) < 40:
            continue
        
        # Split into windows of 500 chars with 60 overlap
        start = 0
        text_len = len(text)
        chunk_idx = 0
        while start < text_len:
            end = min(start + 500, text_len)
            if end < text_len:
                break_pt = text.rfind("\n", start, end)
                if break_pt == -1:
                    break_pt = text.rfind(". ", start, end)
                if break_pt == -1:
                    break_pt = text.rfind(" ", start, end)
                if break_pt > start + 100:
                    end = break_pt + 1
            
            sub = text[start:end].strip()
            if len(sub) > 40:
                header = f"Văn bản chính thức HCMUTE: {clean_title} (Năm học 2026-2027, Trang {page_num}/{total_pages})\n"
                full_text = header + sub
                
                chunks.append({
                    "text": full_text,
                    "metadata": {
                        "type": "hcmute_official_doc",
                        "doc_name": pdf_name,
                        "title": clean_title,
                        "year": "2026-2027",
                        "page": page_num,
                        "total_pages": total_pages,
                        "chunk_index": chunk_idx,
                        "source": pdf_name,
                        "chunk_id": str(uuid.uuid4()),
                        "priority": 1, # Ưu tiên cao nhất vì là học phí mới nhất
                        "language": "vi"
                    }
                })
                chunk_idx += 1
            start = end - 60 if end < text_len else text_len
            
    print(f"Đã tạo {len(chunks)} chunks ngữ cảnh chi tiết từ văn bản học phí mới.")
    return chunks

def main():
    pdf_files = list(PDF_DIR.glob("*.pdf"))
    if not pdf_files:
        print(f"Không tìm thấy file PDF trong {PDF_DIR}")
        return
    
    target_pdf = pdf_files[0]
    print(f"Phát hiện tài liệu học phí mới: {target_pdf.name}")
    
    # 1. OCR
    pages = ocr_pdf_pages(target_pdf)
    
    # Save combined full document markdown
    full_doc = "\n\n---\n\n".join([f"<!-- Trang {p['page']} -->\n" + p['text'] for p in pages if p['text']])
    proc_dir = ai_service_dir / "data" / "processed"
    proc_dir.mkdir(parents=True, exist_ok=True)
    with open(proc_dir / "Quy_dinh_thu_hoc_phi_2026_2027.md", "w", encoding="utf-8") as f:
        f.write(full_doc)
    print(f"Đã lưu văn bản đầy đủ vào {proc_dir / 'Quy_dinh_thu_hoc_phi_2026_2027.md'}")
    
    # 2. Build chunks
    chunks = build_chunks(pages, target_pdf.name)
    
    if not chunks:
        print("Không có chunk nào được tạo.")
        return
    
    # 3. Upsert to Qdrant Cloud
    print("\nTiến hành Embedding và Upsert vào Qdrant Cloud...")
    from vectorstore.qdrant import get_qdrant_client
    from vectorstore.hybrid_index import build_hybrid_qdrant_points, init_sparse_embedder
    from embedding.sparse_embedder import SparseEmbedder
    from core.settings_loader import load_settings
    
    settings = load_settings()
    collection_name = settings["vector_database"]["collection_name"]
    client = get_qdrant_client()
    
    # Load all existing texts from collection to fit sparse embedder comprehensively
    print("Đang nạp toàn bộ văn bản hiện có từ Qdrant Cloud để fit bộ từ vựng Sparse...")
    documents_texts = [c["text"] for c in chunks]
    offset = None
    while True:
        res = client.scroll(
            collection_name=collection_name,
            limit=200,
            offset=offset,
            with_payload=True,
            with_vectors=False
        )
        pts, next_offset = res
        if not pts:
            break
        for pt in pts:
            t = pt.payload.get("text", "")
            if t:
                documents_texts.append(t)
        if next_offset is None:
            break
        offset = next_offset
    
    print(f"Tổng hợp {len(documents_texts)} văn bản. Đang cập nhật Sparse Embedder...")
    sparse = SparseEmbedder()
    sparse.fit(documents_texts)
    init_sparse_embedder(sparse)
    print(f"Bộ từ vựng Sparse Embedder đạt: {len(sparse.vocabulary)} từ.")
    
    # Build hybrid points for the new chunks
    print("Đang sinh vector Dense (Multilingual-E5) và vector Sparse cho chunks mới...")
    points = build_hybrid_qdrant_points(chunks)
    
    # Upsert to Qdrant
    batch_size = 32
    for i in range(0, len(points), batch_size):
        b = points[i:i+batch_size]
        client.upsert(collection_name=collection_name, points=b)
        print(f"Đã nạp batch {i//batch_size + 1} ({len(b)} points) lên Qdrant Cloud.")
        
    print(f"\n🎉 HOÀN TẤT NẠP {len(points)} VECTOR MỚI CỦA NĂM HỌC 2026-2027 LÊN QDRANT CLOUD THÀNH CÔNG!")

if __name__ == "__main__":
    main()
