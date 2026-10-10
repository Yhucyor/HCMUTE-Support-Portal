import logging
import ollama
import time # Thêm thư viện time để đo thời gian thực thi va theo dõi hiệu suất
from llm.prompt import build_prompt
from core.settings_loader import load_settings

settings = load_settings()
logger = logging.getLogger("llm")

LLM_CONFIG = settings["llm"]
MODEL_PROVIDER = LLM_CONFIG.get("provider", "openai")
MODEL_NAME = LLM_CONFIG.get("model_name", "gpt-3.5-turbo")
MODEL_BASE_URL = LLM_CONFIG.get("base_url", "http://localhost:11434")
MODEL_TEMPERATURE = LLM_CONFIG.get("temperature", 0.2)
MODEL_MAX_TOKENS = LLM_CONFIG.get("max_tokens", 1024)
MODEL_TIMEOUT = LLM_CONFIG.get("timeout", 60)

def generate_answer(context: str, question: str) -> str:
    if not context or not context.strip(): # Kiểm tra context rỗng
        logger.warning("Received empty context for answer generation.")
        return "Dữ liệu ngữ cảnh không được để trống."
    
    if not question or not question.strip(): # Kiểm tra câu hỏi rỗng
        logger.warning("Received empty question for answer generation.")
        return "Câu hỏi không được để trống."
    
    prompt = build_prompt(context, question) # Tạo prompt từ context và question
    start = time.time() # Bắt đầu đo thời gian
    
    logger.info(f"Generating answer using model: {MODEL_NAME}")
    
    try:
        if MODEL_PROVIDER == "ollama":
            # Configure ollama client với base_url và timeout
            client = ollama.Client(host=MODEL_BASE_URL, timeout=MODEL_TIMEOUT)
            response = client.chat(
                model=MODEL_NAME,
                messages=[
                    {"role": "system", "content": prompt},
                ],
                options={
                    "temperature": MODEL_TEMPERATURE, # do sang tao
                    "num_predict": MODEL_MAX_TOKENS # so luong token toi da tuc la do dai cau tra loi
                }
            )
            answer = response['message']['content'].strip() # ollama co message chua content con openai co text
        else:
            logger.error(f"Unsupported model provider: {MODEL_PROVIDER}")
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