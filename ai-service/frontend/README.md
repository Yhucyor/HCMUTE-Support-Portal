# Chatbot NMK Frontend

Frontend Next.js cho chatbot NMK Architecture.

## Cài đặt

```bash
cd frontend
npm install
```

## Chạy development server

```bash
npm run dev
```

Truy cập [http://localhost:3000](http://localhost:3000) để xem ứng dụng.

## Cấu hình

Sửa file `.env.local` để thay đổi URL backend:

```
NEXT_PUBLIC_API_URL=http://localhost:8000
```

## Build production

```bash
npm run build
npm start
```

## Tính năng

- Giao diện chat hiện đại với Tailwind CSS
- Tích hợp với FastAPI backend
- Hỗ trợ session ID để theo dõi cuộc hội thoại
- Loading states và error handling
- Responsive design
- Dark mode support

## Stack công nghệ

- Next.js 15
- React 19
- TypeScript
- Tailwind CSS
- Axios
- Lucide React Icons
