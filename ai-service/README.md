# NMK Chatbot - RAG-based AI Assistant

Chatbot thông minh sử dụng Retrieval-Augmented Generation (RAG) để trả lời câu hỏi về công ty Nguyen Minh Khang Architects, dự án kiến trúc, nội thất và tin tức.

## Tính năng

- **RAG Architecture**: Kết hợp vector search và LLM để trả lời chính xác
- **Multilingual Embedding**: Hỗ trợ tiếng Việt với `multilingual-e5-small`
- **Local LLM**: Sử dụng Ollama (Qwen 2.5) - không phụ thuộc API bên ngoài
- **Production-Ready**: Timeout, retry, error handling, health checks
- **Docker Support**: Deploy dễ dàng với Docker Compose

## Kiến trúc

```
User Query → Embedding → Vector Search (Qdrant) → Context Retrieval
                                                          ↓
                                                    LLM (Ollama)
                                                          ↓
                                                      Answer
```

### Stack công nghệ:
- **Embedding**: Sentence Transformers (multilingual-e5-small)
- **Vector DB**: Qdrant
- **LLM**: Ollama (Qwen 2.5:3b)
- **Framework**: Python 3.12+

## Yêu cầu hệ thống

- Python 3.12+
- Docker & Docker Compose
- 4GB RAM (tối thiểu)
- Ollama ([cài đặt](https://ollama.ai))

## Cài đặt nhanh

### 1. Clone repository

```bash
git clone <repo-url>
cd chatbot-NMK
```

### 2. Tạo virtual environment

```bash
python3 -m venv env
source env/bin/activate  # macOS/Linux
# env\Scripts\activate  # Windows
```

### 3. Cài đặt dependencies

```bash
pip install -r requirements.txt
```

### 4. Cấu hình environment

```bash
cp .env.example .env
# Chỉnh sửa .env nếu cần
```

### 5. Khởi động Qdrant

```bash
docker compose up -d qdrant
```

### 6. Cài đặt Ollama model

```bash
ollama pull qwen2.5:3b
```

### 7. Ingest dữ liệu vào vector database

```bash
python -m ingestion.pipeline
```

### 8. Chạy chatbot

```bash
python -m api.main
```

## Deploy với Docker

### Deploy toàn bộ stack

```bash
# Tạo file .env từ template
cp .env.example .env

# Khởi động tất cả services
docker compose up -d

# Xem logs
docker compose logs -f chatbot
```

### Health check

```bash
# Check health của services
curl http://localhost:8000/health

# Response:
# {
#   "status": "healthy",
#   "services": {
#     "qdrant": {"status": "up", "collections": 1},
#     "embedding": {"status": "up"},
#     "llm": {"status": "configured"}
#   }
# }
```

## Cấu hình

### Environment Variables (.env)

```bash
# Vector Database
QDRANT_URL=http://localhost:6333
QDRANT_COLLECTION_NAME=nmk_chatbot_collection
QDRANT_TIMEOUT=30

# LLM
LLM_PROVIDER=ollama
LLM_MODEL_NAME=qwen2.5:3b
LLM_BASE_URL=http://localhost:11434
LLM_TIMEOUT=60

# Embedding
EMBEDDING_MODEL=intfloat/multilingual-e5-small
EMBEDDING_DEVICE=cpu

# Security
MAX_QUERY_LENGTH=500
```

### Settings (config/settings.yaml)

Tất cả config trong `config/settings.yaml` có thể override bằng environment variables.

## Data Ingestion

### Chuẩn bị dữ liệu

Đặt file JSON export vào `data/raw/`:

```bash
data/
  raw/
    database_export_2026-01-14T02-32-14.json
  processed/  # Được tạo tự động
```

### Chạy pipeline

```bash
python -m ingestion.pipeline
```

Pipeline sẽ:
1. Load data từ `data/raw/`
2. Chunk documents theo loại
3. Generate embeddings
4. Upsert vào Qdrant

## Testing

### Manual testing

```bash
python -m api.main
# Nhập câu hỏi: "công ty NMK làm gì?"
```

### Health check

```bash
python -c "from api.health import check_health; print(check_health())"
```

## Cấu trúc thư mục

```
chatbot-NMK/
├── api/                    # API endpoints
│   ├── main.py            # Entry point
│   ├── routes/            # Route handlers
│   └── health.py          # Health check
├── config/                # Configuration files
│   ├── settings.yaml      # App settings
│   └── logging.yaml       # Logging config
├── core/                  # Core utilities
│   ├── schema.py          # Data schemas
│   ├── settings_loader.py # Config loader
│   └── logging_setup.py   # Logging setup
├── embedding/             # Embedding logic
│   └── embedder.py        # Text embedding
├── ingestion/             # Data ingestion
│   ├── pipeline.py        # Main pipeline
│   └── chunking/          # Chunking strategies
├── llm/                   # LLM integration
│   ├── generator.py       # Answer generation
│   └── prompt.py          # Prompt templates
├── retrieval/             # Vector search
│   └── retriever.py       # Document retrieval
├── vectorstore/           # Vector DB
│   ├── qdrant.py          # Qdrant client
│   └── upsert.py          # Data upsert
├── data/                  # Data storage
├── logs/                  # Application logs
├── docker-compose.yml     # Docker orchestration
├── Dockerfile             # Container image
├── requirements.txt       # Python dependencies
└── README.md             # This file
```

## Troubleshooting

### Qdrant connection refused

```bash
# Kiểm tra Qdrant đang chạy
docker ps | grep qdrant

# Khởi động nếu chưa có
docker compose up -d qdrant

# Check logs
docker logs qdrant
```

### Ollama not found

```bash
# Cài đặt Ollama: https://ollama.ai
# Pull model
ollama pull qwen2.5:3b

# Kiểm tra
ollama list
```

### Embedding model download chậm

```bash
# Model sẽ tự động download lần đầu
# Cache tại: ~/.cache/huggingface/

# Download trước:
python -c "from sentence_transformers import SentenceTransformer; SentenceTransformer('intfloat/multilingual-e5-small')"
```

### No documents retrieved

```bash
# Kiểm tra collection có data
curl http://localhost:6333/collections/nmk_chatbot_collection

# Nếu points_count = 0, chạy lại pipeline
python -m ingestion.pipeline
```

## Production Deployment

### Checklist

- [ ] Set `APP_ENV=production` in `.env`
- [ ] Configure proper `QDRANT_API_KEY` nếu dùng cloud
- [ ] Set resource limits trong `docker-compose.yml`
- [ ] Enable log rotation
- [ ] Setup monitoring (Prometheus, Grafana)
- [ ] Configure backup cho Qdrant data
- [ ] Setup reverse proxy (Nginx) với SSL

### Resource recommendations

- **Development**: 2 CPU, 4GB RAM
- **Production**: 4+ CPU, 8GB+ RAM
- **Qdrant storage**: ~500MB cho 100 documents

## License

Proprietary - Nguyen Minh Khang Architects

## Contact

- **Developer**: [Your Name]
- **Company**: Nguyen Minh Khang Architects
- **Email**: contact@nmk.vn
