FROM python:3.12-slim

WORKDIR /app

# 1. Cai dat PyTorch CPU-only truoc (nhe hon 2.5GB, khong tai cac goi cuda/nvidia thua)
RUN pip install --no-cache-dir torch --index-url https://download.pytorch.org/whl/cpu

# 2. Cai dat cac dependencies con lai
COPY requirements.txt .
RUN pip install --default-timeout=1000 --no-cache-dir -r requirements.txt

# 3. Copy source code
COPY . .

# 4. Tao thu muc logs
RUN mkdir -p logs

ENV PORT=8000
EXPOSE 8000

# 5. Khoi chay Uvicorn theo cong Render ($PORT)
CMD ["sh", "-c", "uvicorn api.app:app --host 0.0.0.0 --port ${PORT:-8000}"]

