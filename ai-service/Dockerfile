FROM python:3.12-slim

WORKDIR /app

# 1. Cài đặt PyTorch bản CPU-only trước (giúp nhẹ hơn 2.5GB, không tải các file cuda/nvidia thừa)
RUN pip install --no-cache-dir torch --index-url https://download.pytorch.org/whl/cpu

# 2. Cài đặt các thư viện còn lại trực tiếp vào môi trường (không dùng --user để tránh lỗi quyền hạn)
COPY requirements.txt .
RUN pip install --default-timeout=1000 --no-cache-dir -r requirements.txt

# 3. Copy toàn bộ mã nguồn
COPY . .

# 4. Tạo thư mục storage và logs nếu chưa có
RUN mkdir -p qdrant_storage logs

# 5. Cấu hình cổng động nhận từ Render ($PORT)
ENV PORT=8000
EXPOSE 8000

# 6. Khởi chạy Web Server Uvicorn
CMD ["sh", "-c", "uvicorn api.app:app --host 0.0.0.0 --port ${PORT:-8000}"]
