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

class ChatResponse(BaseModel):
    """Chat response model"""
    answer: str = Field(..., description="Bot's answer")
    sources: list = Field(default_factory=list, description="Source documents")
    session_id: str = Field(..., description="Session ID")


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
    
    logger.info(f"Session {session_id}: Received question: {question}")
    
    try:
        # Get BM25 and Reranker from startup
        bm25 = get_bm25()
        reranker = get_reranker()
        
        if bm25 is None:
            logger.error(f"Session {session_id}: BM25 not initialized!")
            raise HTTPException(
                status_code=503,
                detail="Hệ thống chưa sẵn sàng. Vui lòng thử lại sau."
            )
        
        # Step 1: Hybrid retrieval (Dense + BM25)
        logger.info(f"Session {session_id}: Running hybrid retrieval...")
        documents = hybrid_retrieve(question, bm25)
        
        if not documents:
            logger.warning(f"Session {session_id}: No documents retrieved")
            return ChatResponse(
                answer="Tôi không tìm thấy thông tin phù hợp trong dữ liệu hiện có.",
                sources=[],
                session_id=session_id
            )
        
        logger.info(f"Session {session_id}: Retrieved {len(documents)} documents from hybrid search")
        
        # Step 2: Reranking (if available)
        if reranker is not None:
            logger.info(f"Session {session_id}: Reranking documents...")
            documents = reranker.rerank(question, documents, top_k=RERRANKING_TOP_K)
            logger.info(f"Session {session_id}: After reranking: {len(documents)} documents")
        else:
            logger.warning(f"Session {session_id}: Reranker not available, using hybrid scores only")
            documents = documents[:RERRANKING_TOP_K]  # Cut to top K
        
        # Step 3: Build context and generate answer
        context = "\n\n".join(
            f"[{i+1}] {doc.text}\n(Nguồn: {doc.metadata})" 
            for i, doc in enumerate(documents)
        )
        logger.info(f"Session {session_id}: Retrieved {len(documents)} documents")
        
        answer = generate_answer(context, question)
        logger.info(f"Session {session_id}: Generated answer successfully")
        
        sources = [
            {
                "text": doc.text[:200] + "..." if len(doc.text) > 200 else doc.text,
                "metadata": doc.metadata,
                "score": doc.score
            }
            for doc in documents
        ]
        
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
            session_id=session_id
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