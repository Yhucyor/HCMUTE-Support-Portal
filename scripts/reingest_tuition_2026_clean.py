import os
import sys
import uuid
from pathlib import Path
from dotenv import load_dotenv

# Set paths dynamically
workspace_root = Path(__file__).resolve().parent.parent
ai_service_dir = workspace_root / "ai-service"
sys.path.insert(0, str(ai_service_dir))
os.chdir(str(ai_service_dir))

if sys.platform == "win32":
    sys.stdout.reconfigure(encoding="utf-8")
    sys.stderr.reconfigure(encoding="utf-8")

load_dotenv(ai_service_dir / ".env")

from vectorstore.qdrant import get_qdrant_client
from vectorstore.hybrid_index import build_hybrid_qdrant_points, init_sparse_embedder
from embedding.sparse_embedder import SparseEmbedder
from core.settings_loader import load_settings

DOC_NAME = "Ve việc ban hành Quy định thu học phí năm học 2026-2027.pdf"
DOC_TITLE = "Quy định thu học phí năm học 2026-2027 (Số 2807/QĐ-ĐHCNKT)"

def build_curated_chunks():
    chunks_data = []

    # 1. Tổng quan bảng học phí bậc Đại học chính quy năm học 2026-2027
    chunks_data.append({
        "title_sub": "Tổng quan Mức thu học phí Bậc Đại học Chính quy năm học 2026-2027",
        "text": """Văn bản chính thức HCMUTE: Quy định thu học phí năm học 2026-2027 (Số 2807/QĐ-ĐHCNKT ngày 30/07/2026 của Hiệu trưởng Trường Đại học Công nghệ Kỹ thuật TP.HCM - PGS.TS. Lê Hiếu Giang).

I. MỨC THU HỌC PHÍ ĐẠI HỌC HỆ CHÍNH QUY - CHƯƠNG TRÌNH CHUẨN (ĐẠI TRÀ):
1. Khóa 2026 (Tân sinh viên năm học 2026-2027):
• Khối ngành Khoa học xã hội – Quản lý kinh doanh (Nhóm 1):
  - Mức học phí theo học kỳ: 20.000.000 đồng/học kỳ.
  - Mức học phí theo tín chỉ: 1.212.000 đồng/tín chỉ.
• Khối ngành Công nghệ kỹ thuật – Sản xuất chế biến (Nhóm 2):
  - Ngành Công nghệ truyền thông: 20.000.000 đồng/học kỳ (1.067.000 đồng/tín chỉ).
  - Tất cả các ngành Công nghệ kỹ thuật còn lại (CNTT, Cơ khí, Ô tô, Điện tử, Tự động hóa, Hóa học, Thực phẩm, Xây dựng...): 22.000.000 đồng/học kỳ (1.173.000 đồng/tín chỉ).
• Khối ngành Kiến trúc và quy hoạch (Nhóm 3):
  - Mức học phí theo học kỳ: 24.000.000 đồng/học kỳ.
  - Mức học phí theo tín chỉ: 1.394.000 đồng/tín chỉ.

2. Khóa 2025 trở về trước (Sinh viên các khóa cũ):
• Khối ngành Khoa học xã hội – Quản lý kinh doanh (Nhóm 1): 18.000.000 đồng/học kỳ (1.100.000 đồng/tín chỉ).
• Khối ngành Công nghệ kỹ thuật – Sản xuất chế biến (Nhóm 2): 20.000.000 đồng/học kỳ (1.067.000 đồng/tín chỉ).
• Khối ngành Kiến trúc và quy hoạch (Nhóm 3): 22.000.000 đồng/học kỳ (1.277.000 đồng/tín chỉ).

Ghi chú quan trọng:
- Các khóa 2023 trở về sau đóng học phí theo từng học kỳ.
- Sinh viên học ngành Sư phạm được miễn học phí học lần đầu (phải hoàn thành cam kết phục vụ sư phạm).
- Học phí học lại, học cải thiện, học kéo dài, học bằng hai: Tương ứng với mức học phí theo tín chỉ của từng chương trình đào tạo và từng khóa.
- Học phí học phần Giáo dục Quốc phòng và An ninh (GDQP&AN) và Giáo dục Thể chất (GDTC) cho khóa 2024, 2025, 2026: 1.000.000 đồng/tín chỉ (Học phí trọn gói của các khóa 2024, 2025, 2026 không bao gồm GDQP&AN và GDTC)."""
    })

    # 2. Học phí Chương trình Đặc thù
    chunks_data.append({
        "title_sub": "Mức thu học phí Chương trình Đặc thù năm học 2026-2027",
        "text": """Văn bản chính thức HCMUTE: Quy định thu học phí năm học 2026-2027 (Số 2807/QĐ-ĐHCNKT).

II. MỨC THU HỌC PHÍ HỆ CHÍNH QUY - CHƯƠNG TRÌNH ĐẶC THÙ:
1. Khóa 2026:
• Khối ngành Khoa học xã hội – Quản lý kinh doanh (Nhóm 1) (Kinh doanh quốc tế, Thương mại điện tử, Logistics và quản lý chuỗi cung ứng...):
  - Mức học phí theo học kỳ: 21.000.000 đồng/học kỳ.
  - Mức học phí theo tín chỉ: 1.273.000 đồng/tín chỉ.
• Khối ngành Công nghệ kỹ thuật – Sản xuất chế biến (Nhóm 2) (Công nghệ thông tin đặc thù, Kỹ thuật dữ liệu, An toàn thông tin, Kỹ thuật thiết kế vi mạch, Cơ khí, Ô tô, Cơ điện tử, Điện tử viễn thông, Tự động hóa...):
  - Mức học phí theo học kỳ: 23.000.000 đồng/học kỳ.
  - Mức học phí theo tín chỉ: 1.227.000 đồng/tín chỉ.

2. Khóa 2025 trở về trước:
• Khối ngành Khoa học xã hội – Quản lý kinh doanh: 18.500.000 đồng/học kỳ (1.184.000 đồng/tín chỉ).
• Khối ngành Công nghệ kỹ thuật – Sản xuất chế biến: 20.500.000 đồng/học kỳ (1.093.000 đồng/tín chỉ)."""
    })

    # 3. Học phí Chương trình Đào tạo bằng Tiếng Anh và Hệ Việt - Nhật
    chunks_data.append({
        "title_sub": "Mức thu học phí Chương trình Tiếng Anh và Chương trình Việt - Nhật năm học 2026-2027",
        "text": """Văn bản chính thức HCMUTE: Quy định thu học phí năm học 2026-2027 (Số 2807/QĐ-ĐHCNKT).

III. MỨC THU HỌC PHÍ CHƯƠNG TRÌNH TIẾNG ANH VÀ HỆ VIỆT – NHẬT:
1. Chương trình Đào tạo bằng Tiếng Anh:
• Khóa 2026:
  - Khối ngành Công nghệ kỹ thuật – Sản xuất chế biến: 34.000.000 đồng/học kỳ (1.813.000 đồng/tín chỉ).
  - Khối ngành Khoa học xã hội – Quản lý kinh doanh: 31.000.000 đồng/học kỳ (1.876.000 đồng/tín chỉ).
• Khóa 2025:
  - Khối ngành Công nghệ kỹ thuật: 33.000.000 đồng/học kỳ (1.671.000 đồng/tín chỉ).
  - Khối ngành Khoa học xã hội: 30.000.000 đồng/học kỳ (1.818.000 đồng/tín chỉ).
• Khóa 2024 và Khóa 2023 trở về trước:
  - Khối ngành Công nghệ kỹ thuật: 33.000.000 đồng/học kỳ (1.760.000 đồng/tín chỉ).
  - Khối ngành Khoa học xã hội: 30.000.000 đồng/học kỳ (1.714.000 đồng/tín chỉ).
• Chính sách môn Tiếng Anh: Môn Communicative English 1 và Communicative English 2 học phí là 1.845.000 đồng/tín chỉ. Nhà trường hỗ trợ 70% học phí, sinh viên chỉ phải đóng 30% tương đương 554.000 đồng/tín chỉ.

2. Chương trình Đào tạo hệ Việt – Nhật (gồm các ngành: Công nghệ kỹ thuật ô tô, Công nghệ thông tin, Cơ chế tạo máy, Kỹ thuật nhiệt, Kỹ thuật điện tử viễn thông):
• Khóa 2026: 29.000.000 đồng/học kỳ (1.450.000 đồng/tín chỉ).
• Khóa 2025 và 2024: 28.000.000 đồng/học kỳ (1.333.000 đồng/tín chỉ).
• Khóa 2023 trở về trước: 28.000.000 đồng/học kỳ (1.409.000 đồng/tín chỉ)."""
    })

    # 4. Học phí Chương trình Chất lượng cao (CLC các khóa 2023 trở về trước)
    chunks_data.append({
        "title_sub": "Mức thu học phí Chương trình Chất lượng cao (CLC)",
        "text": """Văn bản chính thức HCMUTE: Quy định thu học phí năm học 2026-2027 (Số 2807/QĐ-ĐHCNKT).

IV. MỨC THU HỌC PHÍ CHƯƠNG TRÌNH CHẤT LƯỢNG CAO (Áp dụng các khóa 2023 trở về trước):
• Khối ngành Công nghệ kỹ thuật – Sản xuất chế biến (Nhóm 2):
  - Mức học phí theo học kỳ: 23.200.000 đồng/học kỳ.
  - Mức học phí theo tín chỉ: 1.237.000 đồng/tín chỉ.
• Khối ngành Khoa học xã hội – Quản lý kinh doanh (Nhóm 1):
  - Mức học phí theo học kỳ: 20.800.000 đồng/học kỳ.
  - Mức học phí theo tín chỉ: 1.189.000 đồng/tín chỉ.
• Ghi chú: Học phí học lại, học cải thiện, học kéo dài theo mức tín chỉ từng chương trình đào tạo."""
    })

    # 5. Chi tiết danh mục các ngành thuộc Khối Kỹ thuật - Công nghệ (Nhóm 2) và Mức học phí
    chunks_data.append({
        "title_sub": "Học phí Khối ngành Công nghệ Kỹ thuật (CNTT, Ô tô, Cơ khí, Điện tử, Tự động hóa...)",
        "text": """Văn bản chính thức HCMUTE: Quy định thu học phí năm học 2026-2027 (Số 2807/QĐ-ĐHCNKT - Phụ lục danh mục Nhóm 2).

MỨC HỌC PHÍ KHỐI CÔNG NGHỆ KỸ THUẬT – SẢN XUẤT CHẾ BIẾN NĂM HỌC 2026-2027:
Áp dụng cho toàn bộ các ngành đào tạo Kỹ thuật & Công nghệ sau:
1. Công nghệ thông tin (CNTT), An toàn thông tin, Kỹ thuật dữ liệu (Data Engineering).
2. Robot và trí tuệ nhân tạo (AI), Kỹ thuật thiết kế vi mạch (Semiconductor IC Design).
3. Công nghệ kỹ thuật máy tính, Hệ thống nhúng và IoT.
4. Công nghệ kỹ thuật ô tô, Kỹ thuật ô tô.
5. Công nghệ kỹ thuật cơ điện tử (Mechatronics).
6. Công nghệ kỹ thuật cơ khí, Công nghệ chế tạo máy, Cơ khí - Tự động hóa.
7. Công nghệ kỹ thuật điện, điện tử; Công nghệ kỹ thuật điện tử, viễn thông.
8. Công nghệ kỹ thuật điều khiển và tự động hóa (Automation).
9. Công nghệ kỹ thuật nhiệt (Nhiệt - Điện lạnh), Năng lượng tái tạo.
10. Công nghệ kỹ thuật hóa học, Công nghệ thực phẩm, Khoa học thực phẩm & Dinh dưỡng.
11. Công nghệ vật liệu, Công nghệ may, Kỹ nghệ gỗ và nội thất.
12. Công nghệ kỹ thuật môi trường, Vật lý kỹ thuật, Kỹ thuật công nghiệp, Kỹ thuật y sinh.
13. Công nghệ kỹ thuật công trình xây dựng, Quản lý xây dựng, Xây dựng công trình giao thông, Quản lý và vận hành hạ tầng.
14. Công nghệ kỹ thuật in.

MỨC HỌC PHÍ CỤ THỂ NĂM HỌC 2026-2027:
• Chương trình Chuẩn (Đại trà):
  - Khóa 2026: 22.000.000 đồng/học kỳ (1.173.000 đồng/tín chỉ). (Riêng ngành Công nghệ truyền thông: 20.000.000 đồng/học kỳ).
  - Khóa 2025 trở về trước: 20.000.000 đồng/học kỳ (1.067.000 đồng/tín chỉ).
• Chương trình Đặc thù (áp dụng các ngành CNTT, Kỹ thuật dữ liệu, An toàn thông tin, Thiết kế vi mạch, Ô tô, Cơ khí, Cơ điện tử, Tự động hóa...):
  - Khóa 2026: 23.000.000 đồng/học kỳ (1.227.000 đồng/tín chỉ).
  - Khóa 2025 trở về trước: 20.500.000 đồng/học kỳ (1.093.000 đồng/tín chỉ).
• Chương trình Đào tạo bằng Tiếng Anh (Chất lượng cao tiếng Anh):
  - Khóa 2026: 34.000.000 đồng/học kỳ (1.813.000 đồng/tín chỉ).
  - Khóa 2025, 2024, 2023: 33.000.000 đồng/học kỳ.
• Chương trình Việt - Nhật (Áp dụng các ngành Ô tô, CNTT, Cơ chế tạo máy, Nhiệt, Điện tử viễn thông):
  - Khóa 2026: 29.000.000 đồng/học kỳ (1.450.000 đồng/tín chỉ).
  - Khóa 2025, 2024: 28.000.000 đồng/học kỳ (1.333.000 đồng/tín chỉ)."""
    })

    # 6. Chi tiết danh mục các ngành thuộc Khối Kinh tế, Xã hội, Ngôn ngữ, Sư phạm (Nhóm 1)
    chunks_data.append({
        "title_sub": "Học phí Khối ngành Kinh tế, Quản lý, Xã hội, Ngôn ngữ, Sư phạm (Nhóm 1)",
        "text": """Văn bản chính thức HCMUTE: Quy định thu học phí năm học 2026-2027 (Số 2807/QĐ-ĐHCNKT - Phụ lục danh mục Nhóm 1).

MỨC HỌC PHÍ KHỐI KHOA HỌC XÃ HỘI – QUẢN LÝ KINH DOANH NĂM HỌC 2026-2027:
Áp dụng cho toàn bộ các ngành sau:
1. Quản trị kinh doanh, Kinh doanh quốc tế, Thương mại điện tử (E-commerce).
2. Kế toán, Công nghệ tài chính (Fintech).
3. Logistics và quản lý chuỗi cung ứng, Quản lý công nghiệp.
4. Luật, Tâm lý học giáo dục.
5. Ngôn ngữ Anh, Sư phạm tiếng Anh, Sư phạm Công nghệ.
6. Thiết kế đồ họa, Thiết kế thời trang.
7. Quản trị nhà hàng và dịch vụ ăn uống.
8. Môi trường và phát triển bền vững.

MỨC HỌC PHÍ CỤ THỂ NĂM HỌC 2026-2027:
• Chương trình Chuẩn (Đại trà):
  - Khóa 2026: 20.000.000 đồng/học kỳ (1.212.000 đồng/tín chỉ).
  - Khóa 2025 trở về trước: 18.000.000 đồng/học kỳ (1.100.000 đồng/tín chỉ).
• Chương trình Đặc thù (áp dụng các ngành Kinh doanh quốc tế, Thương mại điện tử, Logistics...):
  - Khóa 2026: 21.000.000 đồng/học kỳ (1.273.000 đồng/tín chỉ).
  - Khóa 2025 trở về trước: 18.500.000 đồng/học kỳ (1.184.000 đồng/tín chỉ).
• Chương trình Đào tạo bằng Tiếng Anh (Chất lượng cao tiếng Anh):
  - Khóa 2026: 31.000.000 đồng/học kỳ (1.876.000 đồng/tín chỉ).
  - Khóa 2025, 2024, 2023: 30.000.000 đồng/học kỳ (1.714.000 đồng/tín chỉ).
• Chính sách ưu đãi ngành Sư phạm: Sinh viên học ngành Sư phạm (Sư phạm Tiếng Anh, Sư phạm Công nghệ) được miễn học phí học lần đầu theo quy định nếu ký cam kết phục vụ ngành sư phạm."""
    })

    # 7. Học phí Khối Kiến trúc và Quy hoạch (Nhóm 3)
    chunks_data.append({
        "title_sub": "Học phí Khối ngành Kiến trúc và Quy hoạch (Nhóm 3)",
        "text": """Văn bản chính thức HCMUTE: Quy định thu học phí năm học 2026-2027 (Số 2807/QĐ-ĐHCNKT - Phụ lục danh mục Nhóm 3).

MỨC HỌC PHÍ KHỐI KIẾN TRÚC VÀ QUY HOẠCH NĂM HỌC 2026-2027:
Áp dụng cho các ngành: Kiến trúc, Kiến trúc nội thất, Quy hoạch vùng và đô thị.

MỨC HỌC PHÍ CỤ THỂ NĂM HỌC 2026-2027:
• Khóa 2026 (Tân sinh viên):
  - Mức học phí theo học kỳ: 24.000.000 đồng/học kỳ.
  - Mức học phí theo tín chỉ: 1.394.000 đồng/tín chỉ.
• Khóa 2025 trở về trước:
  - Mức học phí theo học kỳ: 22.000.000 đồng/học kỳ.
  - Mức học phí theo tín chỉ: 1.277.000 đồng/tín chỉ."""
    })

    # 8. Học phí Hệ Vừa làm vừa học (VLVH) và Đào tạo từ xa (ĐTTX)
    chunks_data.append({
        "title_sub": "Học phí Hệ Vừa làm vừa học (VLVH) và Hệ Đào tạo từ xa năm học 2026-2027",
        "text": """Văn bản chính thức HCMUTE: Quy định thu học phí năm học 2026-2027 (Số 2807/QĐ-ĐHCNKT).

V. MỨC THU HỌC PHÍ HỆ VỪA LÀM VỪA HỌC (VLVH) VÀ ĐÀO TẠO TỪ XA (ĐTTX):
1. Hệ Vừa làm vừa học (VLVH):
• Khóa 2026:
  - Ngành Kế toán: 14.000.000 đồng/học kỳ.
  - Các ngành đào tạo khác: 15.000.000 đồng/học kỳ.
• Khóa 2025 trở về trước: 13.500.000 đồng/học kỳ.
• Đào tạo tại trạm liên kết ngoài trường: Khóa 2026 là 15.000.000 đồng/học kỳ; Khóa 2025 trở về trước là 13.500.000 đồng/học kỳ.
• Học phí học lại, học ghép hệ chính quy: 910.000 đồng/tín chỉ.

2. Hệ Đào tạo từ xa (ĐTTX):
• Khóa 2026:
  - Khối ngành Khoa học xã hội – Quản lý kinh doanh: 8.800.000 đồng/học kỳ.
  - Khối ngành Công nghệ kỹ thuật: 10.500.000 đồng/học kỳ.
• Khóa 2025 trở về trước:
  - Khối ngành Khoa học xã hội: 8.000.000 đồng/học kỳ.
  - Khối ngành Công nghệ kỹ thuật: 9.500.000 đồng/học kỳ.
• Đào tạo người đã có bằng đại học (văn bằng 2): 8.800.000 đ/học kỳ (kinh tế) và 10.500.000 đ/học kỳ (kỹ thuật).
• Học phí học lại hệ đào tạo từ xa: 500.000 đồng/tín chỉ."""
    })

    # 9. Học phí Sau đại học (Thạc sĩ, Tiến sĩ)
    chunks_data.append({
        "title_sub": "Học phí Bậc Sau đại học (Thạc sĩ Cao học, Tiến sĩ Nghiên cứu sinh)",
        "text": """Văn bản chính thức HCMUTE: Quy định thu học phí năm học 2026-2027 (Số 2807/QĐ-ĐHCNKT).

VI. MỨC THU HỌC PHÍ BẬC SAU ĐẠI HỌC (THẠC SĨ, TIẾN SĨ) NĂM HỌC 2026-2027:
1. Trình độ Thạc sĩ (Cao học):
• Khóa tuyển sinh năm 2026:
  - Khối ngành Công nghệ kỹ thuật: 25.000.000 đồng/học kỳ (1.300.000 đồng/tín chỉ).
  - Khối ngành Khoa học xã hội, Quản lý kinh doanh: 22.000.000 đồng/học kỳ (1.150.000 đồng/tín chỉ).
• Khóa tuyển sinh năm 2025:
  - Khối Công nghệ kỹ thuật: 23.000.000 đồng/học kỳ (1.200.000 đồng/tín chỉ).
  - Khối Khoa học xã hội: 20.000.000 đồng/học kỳ (1.050.000 đồng/tín chỉ).
• Khóa tuyển sinh năm 2024: 49.000.000 đồng/năm học (học lại 1.050.000 đồng/tín chỉ).
• Khóa tuyển sinh 2022-2023 và học viên bảo lưu quay lại: 21.000.000 đồng/học kỳ.
• Gia hạn, kéo dài thời gian thực hiện luận văn/đề án thạc sĩ: 1.000.000 đồng/tháng.

2. Trình độ Tiến sĩ (Nghiên cứu sinh):
• Khóa tuyển sinh năm 2026: 35.000.000 đồng/học kỳ (70.000.000 đồng/năm học).
• Khóa tuyển sinh năm 2025: 33.000.000 đồng/học kỳ (66.000.000 đồng/năm học).
• Khóa tuyển sinh năm 2024: 60.000.000 đồng/năm học.
• Gia hạn, kéo dài thời gian nghiên cứu sinh: 1.500.000 đồng/tháng."""
    })

    # 10. Học phí Chương trình Liên kết quốc tế (Sunderland, Northampton, Tongmyong)
    chunks_data.append({
        "title_sub": "Học phí Chương trình Liên kết đào tạo quốc tế (Anh, Hàn Quốc)",
        "text": """Văn bản chính thức HCMUTE: Quy định thu học phí năm học 2026-2027 (Số 2807/QĐ-ĐHCNKT).

VII. MỨC THU HỌC PHÍ CHƯƠNG TRÌNH LIÊN KẾT ĐÀO TẠO QUỐC TẾ:
1. Chương trình liên kết với Đại học Sunderland và Đại học Northampton (Vương quốc Anh):
• Khóa 2025, 2026:
  - Năm 1 (Anh ngữ và kiến thức cơ sở): 80.000.000 đồng/năm học.
  - Đại học năm cuối (Năm 4): 170.000.000 đồng/năm học.
• Khóa 2024:
  - Năm 1: 70.000.000 đồng/năm học.
  - Đại học năm cuối (Năm 4): 160.000.000 đồng/năm học.
• Khóa 2023:
  - Năm 1: 60.000.000 đồng/năm học.
  - Đại học năm cuối (Năm 4): 150.000.000 đồng/năm học.
• Khóa 2022 về trước:
  - Năm 1: 50.000.000 đồng/năm học.
  - Chương trình BTEC, TMC (Năm 2, 3): 56.000.000 đồng/năm học.
  - Đại học năm cuối: 136.000.000 đồng/năm học.
• Học phí học lại: 3.500.000 đồng/môn (Khóa 2025, 2026) hoặc 2.500.000 đồng/môn (Khóa 2024 trở về trước).

2. Chương trình liên kết với Đại học Tongmyong (Hàn Quốc):
• Khóa 2025: Học lần đầu 80.000.000 đồng/năm học; Học lại 2.200.000 đồng/tín chỉ.
• Khóa 2024: Học lần đầu 70.000.000 đồng/năm học; Học lại 1.950.000 đồng/tín chỉ.
• Khóa 2023: Học lần đầu 60.000.000 đồng/năm học; Học lại 1.680.000 đồng/tín chỉ.
• Khóa 2022 về trước: 38.000.000 đồng/năm học; Học lại 1.050.000 đồng/tín chỉ."""
    })

    # 11. Quy định thời hạn nộp học phí, kênh thanh toán và chế tài nợ học phí
    chunks_data.append({
        "title_sub": "Quy định Hướng dẫn đóng học phí, Kênh thanh toán và Thời hạn nộp học phí",
        "text": """Văn bản chính thức HCMUTE: Quy định thu học phí năm học 2026-2027 (Số 2807/QĐ-ĐHCNKT).

VIII. THỜI GIAN, PHƯƠNG THỨC THANH TOÁN VÀ XỬ LÝ NỢ HỌC PHÍ:
1. Hướng dẫn và Kênh thanh toán học phí:
• Tra cứu thông tin học phí tại website Phòng Kế hoạch Tài chính: http://fpo.hcmute.edu.vn/ (Vào mục THANH TOÁN ONLINE -> HƯỚNG DẪN THANH TOÁN HỌC PHÍ -> HỌC PHÍ HỌC KỲ).
• Cổng thanh toán trực tuyến: https://e-bills.vn/pay/hcmute.
• Hoặc đóng học phí qua các ứng dụng ngân hàng liên kết bằng mã sinh viên (MSSV).

2. Thời gian thu học phí:
• Mỗi học kỳ hệ chính quy được thu làm 02 đợt:
  - Đợt 1: Thu học phí học kỳ và học phí các học phần đã đăng ký đầu học kỳ.
  - Đợt 2: Thu học phí các học phần đăng ký bổ sung trong học kỳ.

3. Chế tài xử lý sinh viên nợ học phí:
• Trước thời gian đăng ký môn học, sinh viên còn nợ học phí của các học kỳ trước sẽ KHÔNG ĐƯỢC PHÉP ĐĂNG KÝ MÔN HỌC của học kỳ tiếp theo.
• Sinh viên không hoàn thành nghĩa vụ học phí đúng hạn sẽ bị xử lý kỷ luật theo Quy chế công tác Học sinh, Sinh viên của Trường."""
    })

    # 12. Học phí Học lại, Học cải thiện, GDQP&AN, GDTC chi tiết
    chunks_data.append({
        "title_sub": "Học phí Học lại, Học cải thiện, Giáo dục quốc phòng an ninh (GDQP) và Thể chất (GDTC)",
        "text": """Văn bản chính thức HCMUTE: Quy định thu học phí năm học 2026-2027 (Số 2807/QĐ-ĐHCNKT).

IX. QUY ĐỊNH HỌC PHÍ HỌC LẠI, HỌC PHẦN GDQP&AN VÀ GDTC:
1. Học phần Giáo dục Quốc phòng và An ninh (GDQP&AN) và Giáo dục Thể chất (GDTC):
• Áp dụng cho sinh viên các khóa 2024, 2025, 2026: Đơn giá học phần GDQP&AN và GDTC là 1.000.000 đồng/tín chỉ.
• Lưu ý: Khung chương trình học phí trọn gói của các khóa 2024, 2025, 2026 không bao gồm học phần GDQP&AN và GDTC; sinh viên đăng ký học phần này sẽ đóng học phí riêng theo đơn giá 1.000.000 đ/tín chỉ.
• Học lại học phần GDQP&AN, GDTC: Áp dụng theo mức thu học phí của khóa 2026 (1.000.000 đồng/tín chỉ).

2. Học phí học lại, học cải thiện, học kéo dài, học bằng thứ hai (chương trình 2):
• Tương ứng với mức học phí theo tín chỉ của từng chương trình đào tạo và từng khóa học mà sinh viên theo học:
  - Hệ Chuẩn khóa 2026: Khối Kỹ thuật 1.173.000 đ/tín chỉ; Khối Kinh tế 1.212.000 đ/tín chỉ; Kiến trúc 1.394.000 đ/tín chỉ.
  - Hệ Chuẩn khóa 2025 trở về trước: Khối Kỹ thuật 1.067.000 đ/tín chỉ; Khối Kinh tế 1.100.000 đ/tín chỉ; Kiến trúc 1.277.000 đ/tín chỉ.
  - Hệ Đặc thù khóa 2026: Khối Kỹ thuật 1.227.000 đ/tín chỉ; Khối Kinh tế 1.273.000 đ/tín chỉ.
  - Hệ Tiếng Anh khóa 2026: Khối Kỹ thuật 1.813.000 đ/tín chỉ; Khối Kinh tế 1.876.000 đ/tín chỉ.
  - Hệ Việt - Nhật khóa 2026: 1.450.000 đ/tín chỉ."""
    })

    # Prepare chunk objects
    final_chunks = []
    for idx, item in enumerate(chunks_data):
        final_chunks.append({
            "text": item["text"],
            "metadata": {
                "type": "hcmute_official_doc",
                "doc_name": DOC_NAME,
                "title": f"{DOC_TITLE} - {item['title_sub']}",
                "year": "2026-2027",
                "page": idx + 1,
                "total_pages": len(chunks_data),
                "chunk_index": idx,
                "source": DOC_NAME,
                "chunk_id": str(uuid.uuid4()),
                "priority": 1,
                "language": "vi"
            }
        })
    return final_chunks

def main():
    print("=== BẮT ĐẦU RE-INGEST BỘ DỮ LIỆU HỌC PHÍ 2026-2027 HOÀN CHỈNH ===")
    settings = load_settings()
    collection_name = settings["vector_database"]["collection_name"]
    client = get_qdrant_client()

    # 1. Thu thập ID các point cũ thuộc tài liệu 2807
    print("1. Tìm kiếm và xóa 101 chunks cũ bị cắt vụn trong Qdrant Cloud...")
    old_point_ids = []
    offset = None
    while True:
        res = client.scroll(
            collection_name=collection_name,
            limit=200,
            offset=offset,
            with_payload=True,
            with_vectors=False
        )
        pts, next_offset = res
        if not pts:
            break
        for pt in pts:
            if pt.payload.get("type") == "hcmute_official_doc" and pt.payload.get("year") == "2026-2027":
                old_point_ids.append(pt.id)
        if next_offset is None:
            break
        offset = next_offset

    if old_point_ids:
        print(f"Phát hiện {len(old_point_ids)} points cũ. Đang xóa...")
        client.delete(
            collection_name=collection_name,
            points_selector=old_point_ids
        )
        print("✅ Đã xóa sạch các chunks cũ bị cắt vụn thành công!")
    else:
        print("Không có point cũ nào cần xóa.")

    # 2. Xây dựng các chunks mới chất lượng cao
    print("\n2. Tạo các semantic chunks hoàn chỉnh có đầy đủ tên ngành và biểu giá...")
    curated_chunks = build_curated_chunks()
    print(f"✅ Đã tạo {len(curated_chunks)} curated chunks chuẩn xác.")

    # 3. Cập nhật Sparse Embedder
    print("\n3. Cập nhật bộ từ vựng Sparse Embedder...")
    documents_texts = [c["text"] for c in curated_chunks]
    offset = None
    while True:
        res = client.scroll(
            collection_name=collection_name,
            limit=200,
            offset=offset,
            with_payload=True,
            with_vectors=False
        )
        pts, next_offset = res
        if not pts:
            break
        for pt in pts:
            t = pt.payload.get("text", "")
            if t:
                documents_texts.append(t)
        if next_offset is None:
            break
        offset = next_offset

    sparse = SparseEmbedder()
    sparse.fit(documents_texts)
    init_sparse_embedder(sparse)
    print(f"Bộ từ vựng Sparse Embedder đạt: {len(sparse.vocabulary)} từ.")

    # 4. Sinh hybrid points và nạp vào Qdrant
    print("\n4. Sinh Dense (Multilingual-E5) + Sparse vector cho các curated chunks...")
    points = build_hybrid_qdrant_points(curated_chunks)
    client.upsert(collection_name=collection_name, points=points)
    print(f"✅ Đã nạp thành công {len(points)} curated points lên Qdrant Cloud!")

    print("\n🎉 HOÀN TẤT CẬP NHẬT DỮ LIỆU HỌC PHÍ MỚI NHẤT 2026-2027!")

if __name__ == "__main__":
    main()
