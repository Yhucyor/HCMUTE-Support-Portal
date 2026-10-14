import logging
import ollama
import google.generativeai as genai
import os
import time # Thêm thư viện time để đo thời gian thực thi va theo dõi hiệu suất
from llm.prompt import build_prompt
from core.settings_loader import load_settings

settings = load_settings()
logger = logging.getLogger("llm")

LLM_CONFIG = settings["llm"]
MODEL_PROVIDER = LLM_CONFIG.get("provider", "gemini")
MODEL_NAME = LLM_CONFIG.get("model_name", "gemini-1.5-flash")
MODEL_BASE_URL = LLM_CONFIG.get("base_url", "http://localhost:11434")
GEMINI_API_KEY = os.getenv("GEMINI_API_KEY", "")

if GEMINI_API_KEY:
    genai.configure(api_key=GEMINI_API_KEY)
MODEL_TEMPERATURE = LLM_CONFIG.get("temperature", 0.2)
MODEL_MAX_TOKENS = LLM_CONFIG.get("max_tokens", 1024)
MODEL_TIMEOUT = LLM_CONFIG.get("timeout", 60)

def generate_answer(context: str, question: str, provider: str = None) -> str:
    if not context or not context.strip(): # Kiểm tra context rỗng
        logger.warning("Received empty context for answer generation.")
        return "Dữ liệu ngữ cảnh không được để trống."
    
    if not question or not question.strip(): # Kiểm tra câu hỏi rỗng
        logger.warning("Received empty question for answer generation.")
        return "Câu hỏi không được để trống."
    
    selected_provider = (provider or MODEL_PROVIDER or "ollama").lower()
    selected_model = "gemini-2.5-flash" if selected_provider == "gemini" else (MODEL_NAME if selected_provider == "ollama" else MODEL_NAME)

    prompt = build_prompt(context, question) # Tạo prompt từ context và question
    start = time.time() # Bắt đầu đo thời gian
    
    logger.info(f"Generating answer using provider: {selected_provider}, model: {selected_model}")
    
    try:
        if selected_provider == "ollama":
            # Configure ollama client với base_url và timeout
            client = ollama.Client(host=MODEL_BASE_URL, timeout=MODEL_TIMEOUT)
            response = client.chat(
                model=selected_model,
                messages=[
                    {"role": "system", "content": prompt},
                ],
                options={
                    "temperature": MODEL_TEMPERATURE, # do sang tao
                    "num_predict": MODEL_MAX_TOKENS # so luong token toi da tuc la do dai cau tra loi
                }
            )
            answer = response['message']['content'].strip() # ollama co message chua content con openai co text
        elif selected_provider == "gemini":
            api_key = os.getenv("GEMINI_API_KEY", GEMINI_API_KEY)
            if not api_key:
                return "Lỗi: Chưa cấu hình GEMINI_API_KEY trong file .env"
            
            genai.configure(api_key=api_key)
            model = genai.GenerativeModel(selected_model)
            response = model.generate_content(
                prompt,
                generation_config=genai.types.GenerationConfig(
                    temperature=MODEL_TEMPERATURE,
                    max_output_tokens=MODEL_MAX_TOKENS,
                )
            )
            answer = response.text.strip()
        else:
            logger.error(f"Unsupported model provider: {selected_provider}")
            return "Nhà cung cấp mô hình không được hỗ trợ."
        
        logger.info("Answer generation completed successfully.")
        logger.info(f"Time taken for generation: {time.time() - start:.2f} seconds")
        return answer
    
    except ollama.ResponseError as e:
        logger.error(f"Ollama API error: {e}")
        return "Xin lỗi, mô hình ngôn ngữ đang gặp vấn đề. Vui lòng thử lại sau."
    except ollama.RequestError as e:
        logger.error(f"Cannot connect to Ollama: {e}")
        return "Không thể kết nối đến dịch vụ AI. Vui lòng kiểm tra cấu hình."
    except TimeoutError:
        logger.error(f"Ollama request timeout after {MODEL_TIMEOUT}s")
        return "Yêu cầu xử lý quá lâu. Vui lòng thử lại với câu hỏi ngắn gọn hơn."
    except Exception as e:
        logger.error(f"Error during answer generation: {e}", exc_info=True)
        return "Đã xảy ra lỗi trong quá trình tạo câu trả lời."