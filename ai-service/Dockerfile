FROM python:3.12-slim

WORKDIR /app

# Cai dat curl cho healthcheck
RUN apt-get update && apt-get install -y --no-install-recommends \
    curl \
    && rm -rf /var/lib/apt/lists/*

# Cai dat dependencies
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

# Copy ma nguon
COPY . .

# Tao thu muc storage cho Qdrant local neu can
RUN mkdir -p qdrant_storage logs

EXPOSE 8000

# Health check endpoint
HEALTHCHECK --interval=30s --timeout=10s --start-period=15s --retries=3 \
  CMD curl -f http://localhost:8000/health || exit 1

# Khoi chay FastAPI server voi Uvicorn
CMD ["uvicorn", "api.app:app", "--host", "0.0.0.0", "--port", "8000"]
