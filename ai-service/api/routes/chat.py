import logging
import os
import time
from typing import Optional
from fastapi import APIRouter, HTTPException, Request
from pydantic import BaseModel, Field
import uuid

from retrieval.hybrid_retriever import hybrid_retrieve
from core.startup import get_bm25, get_reranker
from llm.generator import generate_answer
from core.settings_loader import load_settings

settings = load_settings()
logger = logging.getLogger("chat")
router = APIRouter()

MAX_QUERY_LENGTH = int(os.getenv("MAX_QUERY_LENGTH", "500"))
RATE_LIMIT_PER_MINUTE = int(os.getenv("RATE_LIMIT_PER_MINUTE", "60"))
RERRANKING_TOP_K = settings.get("reranking", {}).get("top_k", 5)

sessions = {}

# Simple in-memory rate limiting
rate_limit_storage = {}  # {ip: [timestamps]}

def check_rate_limit(client_ip: str) -> bool:
    """Check if client has exceeded rate limit"""
    current_time = time.time()
    minute_ago = current_time - 60
    
    if client_ip not in rate_limit_storage:
        rate_limit_storage[client_ip] = []
    
    # Remove timestamps older than 1 minute
    rate_limit_storage[client_ip] = [
        ts for ts in rate_limit_storage[client_ip] if ts > minute_ago
    ]
    
    # Check if exceeded limit
    if len(rate_limit_storage[client_ip]) >= RATE_LIMIT_PER_MINUTE:
        return False
    
    # Add current request
    rate_limit_storage[client_ip].append(current_time)
    return True

class ChatRequest(BaseModel):
    """Chat request model"""
    query: str = Field(..., min_length=1, max_length=MAX_QUERY_LENGTH, description="User's question")
    session_id: Optional[str] = Field(None, description="Session ID for conversation tracking")
    provider: Optional[str] = Field("ollama", description="LLM provider: 'ollama' or 'gemini'")
    history: Optional[list] = Field(default_factory=list, description="Recent conversation history")

class ChatResponse(BaseModel):
    """Chat response model"""
    answer: str = Field(..., description="Bot's answer")
    sources: list = Field(default_factory=list, description="Source documents")
    session_id: str = Field(..., description="Session ID")
    provider: Optional[str] = Field("ollama", description="LLM provider used")


@router.post("/chat", response_model=ChatResponse)
async def chat_endpoint(request: ChatRequest, req: Request):
    # Rate limiting check
    client_ip = req.client.host if req.client else "unknown"
    if not check_rate_limit(client_ip):
        logger.warning(f"Rate limit exceeded for IP: {client_ip}")
        raise HTTPException(
            status_code=429,
            detail=f"Tốc độ request quá nhanh. Vui lòng thử lại sau. (Max {RATE_LIMIT_PER_MINUTE} requests/minute)"
        )
    
    question = request.query.strip()
    
    if not question:
        raise HTTPException(status_code=400, detail="Vui lòng nhập câu hỏi.")
    
    session_id = request.session_id or str(uuid.uuid4())
    provider = (request.provider or "ollama").lower()
    
    logger.info(f"Session {session_id}: Received question: {question} (Provider: {provider})")
    
    try:
        import math
        import re

        # Nhận diện câu chào hỏi, cảm ơn, xã giao cơ bản
        def is_casual(q: str) -> bool:
            clean = re.sub(r"[^\w\s]", "", q.lower()).strip()
            patterns = [
                r"^(h+i+|h+e+l+o+|h+e+l+l+o+|a+l+o+|h+e+y+|y+o+)$",
                r"^(xin chào|chào|chào bạn|chao|chao ban|hi bạn|hi ban)$",
                r"^(bạn là ai|ban la ai|who are you|bạn tên gì|ban ten gi)$",
                r"^(cảm ơn|cảm ơn bạn|cam on|thanks|thank you|tks)$",
                r"^(tạm biệt|tam biet|bye|bye bye|gbye)$",
                r"^(ok|oke|okie|ừ|uh|dạ|da|vâng|vang)$"
            ]
            return any(re.match(p, clean) for p in patterns)

        if is_casual(question):
            return ChatResponse(
                answer="Hi bạn nè! ✨ Mình là Trợ lý AI học vụ của HCMUTE Student Portal. Mình có thể hỗ trợ bạn giải đáp mọi thông tin về học phí, quy chế học vụ, đăng ký môn học và các thủ tục tại trường. Bạn cần mình giải đáp vấn đề gì hôm nay? 😊",
                sources=[],
                session_id=session_id,
                provider=provider
            )

        # Get BM25 and Reranker from startup
        bm25 = get_bm25()
        reranker = get_reranker()
        
        if bm25 is None:
            logger.error(f"Session {session_id}: BM25 not initialized!")
            raise HTTPException(
                status_code=503,
                detail="Hệ thống chưa sẵn sàng. Vui lòng thử lại sau."
            )
        
        history = request.history or []
        search_query = question

        # Step 0: Multi-turn Contextual Query Rewriting (Query Condensation)
        if history and len(history) > 0:
            try:
                from retrieval.query_rewriter import rewrite_query
                rewritten = rewrite_query(question, history)
                if rewritten and rewritten.strip():
                    search_query = rewritten.strip()
                    logger.info(f"Session {session_id}: Rewrote query from '{question}' -> '{search_query}'")
            except Exception as e:
                logger.warning(f"Session {session_id}: Query rewriting failed ({e}), keeping original question.")
                search_query = question

        # Step 1: Hybrid retrieval (Dense + BM25) với search_query độc lập
        logger.info(f"Session {session_id}: Running hybrid retrieval with search query: '{search_query}'...")
        documents = hybrid_retrieve(search_query, bm25)
        
        if not documents:
            logger.warning(f"Session {session_id}: No documents retrieved")
            return ChatResponse(
                answer="Tôi không tìm thấy thông tin phù hợp trong dữ liệu hiện có.",
                sources=[],
                session_id=session_id,
                provider=provider
            )
        
        logger.info(f"Session {session_id}: Retrieved {len(documents)} documents from hybrid search")
        
        # Step 2: Reranking (if available) với search_query độc lập
        if reranker is not None:
            logger.info(f"Session {session_id}: Reranking documents with query: '{search_query}'...")
            documents = reranker.rerank(search_query, documents, top_k=RERRANKING_TOP_K)
            logger.info(f"Session {session_id}: After reranking: {len(documents)} documents")
        else:
            logger.warning(f"Session {session_id}: Reranker not available, using hybrid scores only")
            documents = documents[:RERRANKING_TOP_K]  # Cut to top K
        
        # Step 3: Build context and generate answer (kèm history cho câu trả lời tự nhiên, liền mạch)
        context = "\n\n".join(
            f"[{i+1}] {doc.text}\n(Nguồn: {doc.metadata})" 
            for i, doc in enumerate(documents)
        )
        logger.info(f"Session {session_id}: Retrieved {len(documents)} documents")
        
        answer = generate_answer(context, question, provider=provider, history=history)
        logger.info(f"Session {session_id}: Generated answer successfully via {provider}")

        def clean_source_title(meta: dict) -> str:
            raw_title = meta.get("title") or meta.get("source") or "Văn bản quy định HCMUTE"
            if raw_title.lower().endswith(".pdf"):
                raw_title = raw_title[:-4]
            cleaned = re.sub(r"^\d+[\.\-_]\s*", "", raw_title)
            return cleaned.strip() or "Văn bản quy định HCMUTE"

        def normalize_score(raw_score) -> float:
            if raw_score is None:
                return 0.85
            try:
                val = 1.0 / (1.0 + math.exp(-float(raw_score) / 2.0))
                return round(min(0.98, max(0.40, val)), 2)
            except Exception:
                return 0.85

        # Khử trùng lặp nguồn (Deduplication) và lọc theo ngưỡng chất lượng (Relevance Threshold)
        seen_titles = set()
        sources = []
        for doc in documents:
            title = clean_source_title(doc.metadata or {})
            if not title or title.lower() in seen_titles:
                continue

            score = normalize_score(doc.score)
            # Chỉ hiển thị nguồn có liên quan thực sự (trên 68%)
            if score < 0.68:
                continue

            seen_titles.add(title.lower())
            sources.append({
                "text": doc.text[:250] + "..." if len(doc.text) > 250 else doc.text,
                "title": title,
                "source": (doc.metadata or {}).get("source") or title,
                "page_number": (doc.metadata or {}).get("page"),
                "document_type": (doc.metadata or {}).get("type"),
                "score": score,
                "metadata": doc.metadata
            })
        
        if session_id not in sessions:
            sessions[session_id] = []
        sessions[session_id].append({
            "question": question,
            "answer": answer,
            "sources": sources
        })
        
        return ChatResponse(
            answer=answer,
            sources=sources,
            session_id=session_id,
            provider=provider
        )
    
    except Exception as e:
        logger.error(f"Session {session_id}: Error in chat: {e}", exc_info=True)
        raise HTTPException(
            status_code=500,
            detail="Xin lỗi, đã xảy ra lỗi khi xử lý câu hỏi của bạn. Vui lòng thử lại sau."
        )


def chat(question: str) -> str:
    """Legacy CLI chat function - now uses hybrid retrieval"""
    if not question or not question.strip():
        logger.warning("Empty question received")
        return "Vui lòng nhập câu hỏi."
    
    if len(question) > MAX_QUERY_LENGTH:
        logger.warning(f"Query too long: {len(question)} characters")
        return f"Câu hỏi quá dài. Vui lòng giới hạn dưới {MAX_QUERY_LENGTH} ký tự."
    
    logger.info(f"Received question: {question}")
    
    try:
        bm25 = get_bm25()
        reranker = get_reranker()
        
        if bm25 is None:
            return "Hệ thống chưa sẵn sàng. Vui lòng thử lại sau."
        
        # Hybrid retrieval
        documents = hybrid_retrieve(question, bm25)
        
        if not documents:
            logger.warning("No documents retrieved for the question")
            return "Tôi không tìm thấy thông tin phù hợp trong dữ liệu hiện có."
        
        # Reranking
        if reranker is not None:
            documents = reranker.rerank(question, documents, top_k=RERRANKING_TOP_K)
        else:
            documents = documents[:RERRANKING_TOP_K]
        
        context = "\n\n".join(f"[{i+1}] {doc.text}\n(Nguồn: {doc.metadata})" for i, doc in enumerate(documents))
        logger.info(f"Retrieved {len(documents)} documents for the question")
            
        answer = generate_answer(context, question)
        logger.info("Generated answer successfully")
        return answer
    
    except Exception as e:
        logger.error(f"Error in chat function: {e}", exc_info=True)
        return "Xin lỗi, đã xảy ra lỗi khi xử lý câu hỏi của bạn. Vui lòng thử lại sau."