import logging
import time
from contextlib import asynccontextmanager
from fastapi import FastAPI, Request
from fastapi.middleware.cors import CORSMiddleware
from core.logging_setup import setup_logging
from core.startup import initialize_rag_components

setup_logging()
logger = logging.getLogger("api")

@asynccontextmanager
async def lifespan(app: FastAPI):
    """Startup and shutdown events"""
    # Startup
    logger.info("Starting HCMUTE Chatbot API...")
    try:
        initialize_rag_components()
        logger.info("Application startup completed")
    except Exception as e:
        logger.error(f"Startup failed: {e}", exc_info=True)
    
    yield
    
    # Shutdown
    logger.info("Shutting down HCMUTE Chatbot API...")

app = FastAPI(
    title="HCMUTE Chatbot API",
    description="API for HCMUTE Student Portal Chatbot",
    version="1.0.0",
    lifespan=lifespan
)

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],  
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

@app.middleware("http")
async def track_response_time(request: Request, call_next):
    """Track response time for all requests"""
    start_time = time.time()
    response = await call_next(request)
    duration = time.time() - start_time
    response.headers["X-Response-Time"] = f"{duration:.3f}s"
    logger.info(f"{request.method} {request.url.path} took {duration:.3f}s")
    return response

from api.routes import chat_router, health_router

app.include_router(health_router, tags=["health"])
app.include_router(chat_router, prefix="/api", tags=["chat"])

@app.get("/")
async def root():
    """Root endpoint"""
    return {
        "message": "HCMUTE Chatbot API",
        "version": "1.0.0",
        "docs": "/docs"
    }

if __name__ == "__main__":
    import uvicorn
    uvicorn.run("api.app:app", host="0.0.0.0", port=8000, reload=True)
