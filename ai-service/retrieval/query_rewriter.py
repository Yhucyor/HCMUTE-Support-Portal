import os
import re
import time
import logging
from typing import List, Dict

logger = logging.getLogger("query_rewriter")

# System prompt tối ưu cho Query Condensation / Rewriting
REWRITE_SYSTEM_PROMPT = """Bạn là chuyên gia tinh chỉnh câu hỏi tìm kiếm (Contextual Query Rewriter) cho hệ sinh thái học vụ HCMUTE.
Nhiệm vụ: Phân tích lịch sử hội thoại gần nhất và câu hỏi tiếp theo của sinh viên để viết lại câu hỏi tiếp theo thành MỘT CÂU HỎI ĐỘC LẬP, ĐẦY ĐỦ CHỦ NGỮ - VỊ NGỮ để đưa vào bộ tìm kiếm tài liệu (Hybrid Search / Vector Database).

Quy tắc bắt buộc:
1. Chỉ xuất đúng 1 câu hỏi đã viết lại trên 1 dòng duy nhất. Không giải thích, không thêm dấu ngoặc kép, không chào hỏi.
2. Bổ sung các thực thể bị khuyết thiếu từ lượt trước (tên ngành học, khóa tuyển sinh, hệ đào tạo, học phần, thủ tục...).
3. NẾU câu hỏi mới ĐÃ ĐẦY ĐỦ VÀ ĐỘC LẬP, hoặc là chủ đề hoàn toàn mới: HÃY TRẢ VỀ NGUYÊN VĂN CÂU HỎI MỚI.
4. Giữ nguyên ngôn ngữ tiếng Việt tự nhiên, chính xác.

Ví dụ mẫu:
Lịch sử:
User: Học phí ngành Kỹ thuật ô tô khóa 2026 bao nhiêu?
Assistant: Mức học phí hệ đại trà ngành Kỹ thuật ô tô khóa 2026 là khoảng 32 triệu đồng/năm.
Follow-up: Thế còn hệ chất lượng cao tiếng Anh?
-> Mức học phí chương trình chất lượng cao tiếng Anh ngành Kỹ thuật ô tô khóa 2026 là bao nhiêu?

Lịch sử:
User: Thủ tục xin miễn giảm học phí cần những giấy tờ gì?
Assistant: Cần đơn xin miễn giảm theo mẫu, bản sao công chứng sổ hộ nghèo hoặc giấy xác nhận đối tượng chính sách.
Follow-up: Nộp ở phòng nào và hạn chót là khi nào?
-> Nộp hồ sơ xin miễn giảm học phí ở phòng nào và hạn chót nộp là khi nào?
"""

def format_history_for_rewriter(history: List[Dict[str, str]], max_turns: int = 3) -> str:
    """Format tối đa max_turns lượt trao đổi gần nhất, rút gọn văn bản để tiết kiệm token và tăng tốc độ"""
    if not history:
        return ""
    
    # Lấy các tin nhắn gần nhất
    recent = history[- (max_turns * 2):]
    formatted_lines = []
    
    for item in recent:
        role = item.get("role", "user").lower()
        role_label = "User" if role == "user" else "Assistant"
        content = item.get("content", "").strip()
        # Truncate câu trả lời của Assistant để prompt siêu nhẹ
        if role_label == "Assistant" and len(content) > 120:
            content = content[:120].rsplit(" ", 1)[0] + "..."
        elif len(content) > 150:
            content = content[:150]
        
        if content:
            formatted_lines.append(f"{role_label}: {content}")
            
    return "\n".join(formatted_lines)

def rewrite_query(question: str, history: List[Dict[str, str]]) -> str:
    """
    Kỹ thuật Query Condensation:
    Chuyển câu hỏi phụ thuộc ngữ cảnh (Follow-up query) thành câu hỏi độc lập (Standalone query)
    """
    clean_q = question.strip()
    if not history or len(history) == 0:
        return clean_q

    # Nếu câu hỏi quá dài (>35 từ) hoặc có cấu trúc rất đầy đủ, khả năng cao là câu hỏi độc lập
    words = clean_q.split()
    if len(words) > 35:
        return clean_q

    # Kiểm tra xem có dấu hiệu câu hỏi tiếp diễn không
    continuation_indicators = [
        "thế còn", "vậy còn", "còn", "thế", "vậy", "thì sao", "đó", "nó", "ở đâu", 
        "bao nhiêu", "khi nào", "thủ tục", "hệ này", "ngành này", "ngành đó", "môn này"
    ]
    lower_q = clean_q.lower()
    is_likely_continuation = any(ind in lower_q for ind in continuation_indicators) or len(words) <= 7

    # Nếu câu hỏi dài và không có dấu hiệu phụ thuộc, trả về nguyên bản
    if not is_likely_continuation and len(words) >= 15:
        return clean_q

    history_text = format_history_for_rewriter(history)
    if not history_text:
        return clean_q

    start_time = time.time()
    
    # 1. Thử rewrite bằng Gemini Flash (cực nhanh ~300-600ms)
    gemini_api_key = os.getenv("GEMINI_API_KEY")
    if not gemini_api_key:
        try:
            from dotenv import load_dotenv
            from pathlib import Path
            env_path = Path(__file__).resolve().parent.parent / ".env"
            if env_path.exists():
                load_dotenv(env_path)
                gemini_api_key = os.getenv("GEMINI_API_KEY")
        except Exception:
            pass

    if gemini_api_key:
        try:
            import google.generativeai as genai
            genai.configure(api_key=gemini_api_key)
            
            prompt = f"{REWRITE_SYSTEM_PROMPT}\n\nLịch sử hội thoại:\n{history_text}\n\nFollow-up: {clean_q}\n\nStandalone Query:"
            
            for m_name in ["gemini-2.5-flash", "gemini-3.5-flash", "gemini-2.5-pro"]:
                try:
                    model = genai.GenerativeModel(m_name)
                    response = model.generate_content(
                        prompt,
                        generation_config=genai.types.GenerationConfig(
                            temperature=0.0,
                            max_output_tokens=1024
                        )
                    )
                    if response and response.text:
                        rewritten = response.text.strip().replace('"', '').replace("->", "").strip()
                        first_line = rewritten.split("\n")[0].strip()
                        if first_line and len(first_line) >= 5:
                            elapsed = time.time() - start_time
                            logger.info(f"⚡ Rewriter thành công ({elapsed:.2f}s, model {m_name}): '{clean_q}' -> '{first_line}'")
                            return first_line
                except Exception as m_err:
                    logger.debug(f"Model {m_name} failed rewrite: {m_err}")
                    continue
        except Exception as e:
            logger.warning(f"Gemini rewriter gặp lỗi ({e}), chuyển sang heuristic rewriter")

    # 2. Fallback heuristic rewriter nếu Gemini không khả dụng
    try:
        # Lấy câu hỏi gần nhất của User trong history
        last_user_query = ""
        for item in reversed(history):
            if item.get("role", "").lower() == "user":
                last_user_query = item.get("content", "").strip()
                break
        
        if last_user_query:
            # Trích xuất các thực thể quan trọng từ câu hỏi trước: ngành, năm/khóa
            entity_patterns = [
                r"(ngành\s+[^,?.!\n]+)",
                r"(khóa\s+\d{4})",
                r"(năm\s+\d{4})",
                r"(học phí\s+[^,?.!\n]+)",
                r"(học bổng\s+[^,?.!\n]+)"
            ]
            extracted_entities = []
            for pat in entity_patterns:
                m = re.search(pat, last_user_query, re.IGNORECASE)
                if m:
                    extracted_entities.append(m.group(1).strip())
            
            if extracted_entities:
                combined_entity = " ".join(extracted_entities)
                if not any(ent.lower() in clean_q.lower() for ent in extracted_entities):
                    heuristic_q = f"{clean_q} ({combined_entity})"
                    logger.info(f"Heuristic Rewriter: '{clean_q}' -> '{heuristic_q}'")
                    return heuristic_q
    except Exception as ex:
        logger.warning(f"Heuristic rewriter failed: {ex}")

    return clean_q
