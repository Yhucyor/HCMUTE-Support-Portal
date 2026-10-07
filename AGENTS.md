# 🛡️ AI AGENT RULES: PHẠM VI DEV A (BACKEND) & QUY TẮC GIT TỰ ĐỘNG

> **LƯU Ý ĐẶC BIỆT DÀNH CHO AI**:
> Người dùng repository này là **Dev A (Backend Lead)**.
> **TUYỆT ĐỐI NGHIÊM CẤM** AI tự ý can thiệp, viết đè hoặc chỉnh sửa các file thuộc trách nhiệm của **Dev B (Frontend Lead)**:
> - CẤM sửa HTML Thymeleaf templates (`src/main/resources/templates/**`)
> - CẤM sửa CSS / Styling (`src/main/resources/static/css/**`)
> - CẤM sửa Javascript giao diện (`src/main/resources/static/js/**`)
> - CẤM can thiệp Media Storage Service (Cloudinary/Local)

---

## 🚀 QUY TẮC GIT COMMIT & PUSH TỰ ĐỘNG:
1. **Trên nhánh tính năng (`feature/*`):**
   - AI **TỰ ĐỘNG** chia nhỏ micro-commits theo chuẩn Conventional Commits.
   - Biên dịch `mvn test-compile` thành công $\rightarrow$ AI **TỰ ĐỘNG commit và push lên remote theo nhánh** mà không cần chờ lệnh người dùng.
2. **Quy tắc gộp nhánh (`merge`):**
   - ⛔ **CHỈ KHI NGƯỜI DÙNG BẢO "HOÀN THIỆN" / RA LỆNH MERGE:**
     * AI mới được phép merge `feature/*` vào `dev` (`git merge --no-ff`), kiểm tra build và push lên `origin dev`.
     * Khi người dùng yêu cầu phát hành: Merge từ `dev` vào `main`, gắn Tag và push `main` + `tags`.
   - ⛔ **TUYỆT ĐỐI KHÔNG TỰ ĐỘNG MERGE VÀO `dev` HOẶC `main` KHI CHƯA CÓ LỆNH HOÀN THIỆN!**
