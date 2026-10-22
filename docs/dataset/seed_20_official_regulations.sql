-- ==============================================================================
-- SQL SCRIPT: NẠP 20 CÔNG VĂN HỌC VỤ TRỌNG TÂM (HCMUTE 2025 - 2026)
-- ĐÃ BỎ CHỮ [CÔNG VĂN QUY CHẾ], TIÊU ĐỀ IN HOA TRANG NHÃ
-- ==============================================================================

-- 1. Dọn dẹp bài viết công văn chunking cũ (ẩn mềm is_deleted = 1)
UPDATE posts SET is_deleted = 1 WHERE post_type = 'OFFICIAL_ANNOUNCEMENT' AND (content LIKE '%🏛️ **THÔNG BÁO VỀ CÔNG VĂN%' OR title LIKE '%[CÔNG VĂN QUY CHẾ]%');

-- 2. Nạp 20 công văn quy chế mới chuẩn hóa
-- [1] THÔNG BÁO XÉT HỌC BỔNG HỖ TRỢ SINH VIÊN HOÀN CẢNH KHÓ KHĂN HỌC KỲ 2 (2025 - 2026)
INSERT INTO posts (title, content, post_type, status, department_id, author_id, approved_by, approved_at, is_pinned, view_count, like_count, is_deleted, created_at, updated_at)
VALUES ('THÔNG BÁO XÉT HỌC BỔNG HỖ TRỢ SINH VIÊN HOÀN CẢNH KHÓ KHĂN HỌC KỲ 2 (2025 - 2026)', '<p>Nhà trường ban hành văn bản chính thức về việc <strong>THÔNG BÁO XÉT HỌC BỔNG HỖ TRỢ SINH VIÊN HOÀN CẢNH KHÓ KHĂN HỌC KỲ 2 (2025 - 2026)</strong>.</p>
<p>Sinh viên vui lòng tải hoặc mở tệp PDF đính kèm bên dưới để theo dõi toàn văn chi tiết, nắm rõ các mốc thời gian quan trọng và thực hiện đúng thời hạn quy định.</p>', 'OFFICIAL_ANNOUNCEMENT', 'APPROVED', 3, 4, 4, NOW(), 1, 3520, 428, 0, NOW(), NOW());
SET @last_post_id = LAST_INSERT_ID();
INSERT INTO post_attachments (post_id, file_name, file_url, file_type, file_size, source_type, is_deleted, created_at, updated_at)
VALUES (@last_post_id, 'THONG_BAO_XET_HOC_BONG_KHO_KHAN_HK2_2025_2026.pdf', 'D:\\HK5\\CongNghePhanMem\\tailieuAI\\2026\\1. THONG BAO XET HOC BONG HO TRO SV KHO KHAN HKII 25-26 - Signed (4).pdf', 'PDF', 1055473, 'LOCAL_PATH', 0, NOW(), NOW());

-- [2] QUY ĐỊNH CHUẨN NGOẠI NGỮ ĐẦU RA VÀ CÔNG NHẬN CHỨNG CHỈ QUỐC TẾ (CAMBRIDGE, TOEIC, IELTS)
INSERT INTO posts (title, content, post_type, status, department_id, author_id, approved_by, approved_at, is_pinned, view_count, like_count, is_deleted, created_at, updated_at)
VALUES ('QUY ĐỊNH CHUẨN NGOẠI NGỮ ĐẦU RA VÀ CÔNG NHẬN CHỨNG CHỈ QUỐC TẾ (CAMBRIDGE, TOEIC, IELTS)', '<p>Nhà trường ban hành văn bản chính thức về việc <strong>QUY ĐỊNH CHUẨN NGOẠI NGỮ ĐẦU RA VÀ CÔNG NHẬN CHỨNG CHỈ QUỐC TẾ (CAMBRIDGE, TOEIC, IELTS)</strong>.</p>
<p>Sinh viên vui lòng tải hoặc mở tệp PDF đính kèm bên dưới để theo dõi toàn văn chi tiết, nắm rõ các mốc thời gian quan trọng và thực hiện đúng thời hạn quy định.</p>', 'OFFICIAL_ANNOUNCEMENT', 'APPROVED', 5, 28, 28, NOW(), 1, 4120, 512, 0, NOW(), NOW());
SET @last_post_id = LAST_INSERT_ID();
INSERT INTO post_attachments (post_id, file_name, file_url, file_type, file_size, source_type, is_deleted, created_at, updated_at)
VALUES (@last_post_id, 'QUY_DINH_CHUAN_NGOAI_NGU_DAU_RA_CAMBRIDGE_TOEIC_IELTS.pdf', 'D:\\HK5\\CongNghePhanMem\\tailieuAI\\2026\\TB công nhận chứng chỉ Cambridge đạt chuẩn ngoại ngữ đầu ra.pdf', 'PDF', 632725, 'LOCAL_PATH', 0, NOW(), NOW());

-- [3] THÔNG BÁO KẾ HOẠCH THI ANH VĂN ĐẦU VÀO NĂM HỌC 2026 - 2027
INSERT INTO posts (title, content, post_type, status, department_id, author_id, approved_by, approved_at, is_pinned, view_count, like_count, is_deleted, created_at, updated_at)
VALUES ('THÔNG BÁO KẾ HOẠCH THI ANH VĂN ĐẦU VÀO NĂM HỌC 2026 - 2027', '<p>Nhà trường ban hành văn bản chính thức về việc <strong>THÔNG BÁO KẾ HOẠCH THI ANH VĂN ĐẦU VÀO NĂM HỌC 2026 - 2027</strong>.</p>
<p>Sinh viên vui lòng tải hoặc mở tệp PDF đính kèm bên dưới để theo dõi toàn văn chi tiết, nắm rõ các mốc thời gian quan trọng và thực hiện đúng thời hạn quy định.</p>', 'OFFICIAL_ANNOUNCEMENT', 'APPROVED', 5, 28, 28, NOW(), 0, 1840, 165, 0, NOW(), NOW());
SET @last_post_id = LAST_INSERT_ID();
INSERT INTO post_attachments (post_id, file_name, file_url, file_type, file_size, source_type, is_deleted, created_at, updated_at)
VALUES (@last_post_id, 'KE_HOACH_THI_ANH_VAN_DAU_VAO_NH_2026_2027.pdf', 'D:\\HK5\\CongNghePhanMem\\tailieuAI\\2026\\14.08.26 - 158.Thông báo KH thi Anh văn đầu vào NH 2026-2027 (2).pdf', 'PDF', 224127, 'LOCAL_PATH', 0, NOW(), NOW());

-- [4] HƯỚNG DẪN XÉT MIỄN VÀ CHUYỂN ĐIỂM HỌC PHẦN NGOẠI NGỮ HỌC KỲ 1
INSERT INTO posts (title, content, post_type, status, department_id, author_id, approved_by, approved_at, is_pinned, view_count, like_count, is_deleted, created_at, updated_at)
VALUES ('HƯỚNG DẪN XÉT MIỄN VÀ CHUYỂN ĐIỂM HỌC PHẦN NGOẠI NGỮ HỌC KỲ 1', '<p>Nhà trường ban hành văn bản chính thức về việc <strong>HƯỚNG DẪN XÉT MIỄN VÀ CHUYỂN ĐIỂM HỌC PHẦN NGOẠI NGỮ HỌC KỲ 1</strong>.</p>
<p>Sinh viên vui lòng tải hoặc mở tệp PDF đính kèm bên dưới để theo dõi toàn văn chi tiết, nắm rõ các mốc thời gian quan trọng và thực hiện đúng thời hạn quy định.</p>', 'OFFICIAL_ANNOUNCEMENT', 'APPROVED', 5, 28, 28, NOW(), 0, 2490, 280, 0, NOW(), NOW());
SET @last_post_id = LAST_INSERT_ID();
INSERT INTO post_attachments (post_id, file_name, file_url, file_type, file_size, source_type, is_deleted, created_at, updated_at)
VALUES (@last_post_id, 'HUONG_DAN_MIEN_CHUYEN_DIEM_NGOAI_NGU_HK1.pdf', 'D:\\HK5\\CongNghePhanMem\\tailieuAI\\2026\\1944Thông báo_Miễn chuyển điểm ngoại ngữ_HK1-NH26-27_FN - Signed (2).pdf', 'PDF', 1077010, 'LOCAL_PATH', 0, NOW(), NOW());

-- [5] HƯỚNG DẪN ĐĂNG KÝ VÀ TRIỂN KHAI ĐỀ TÀI NGHIÊN CỨU KHOA HỌC SINH VIÊN (NCKH)
INSERT INTO posts (title, content, post_type, status, department_id, author_id, approved_by, approved_at, is_pinned, view_count, like_count, is_deleted, created_at, updated_at)
VALUES ('HƯỚNG DẪN ĐĂNG KÝ VÀ TRIỂN KHAI ĐỀ TÀI NGHIÊN CỨU KHOA HỌC SINH VIÊN (NCKH)', '<p>Nhà trường ban hành văn bản chính thức về việc <strong>HƯỚNG DẪN ĐĂNG KÝ VÀ TRIỂN KHAI ĐỀ TÀI NGHIÊN CỨU KHOA HỌC SINH VIÊN (NCKH)</strong>.</p>
<p>Sinh viên vui lòng tải hoặc mở tệp PDF đính kèm bên dưới để theo dõi toàn văn chi tiết, nắm rõ các mốc thời gian quan trọng và thực hiện đúng thời hạn quy định.</p>', 'OFFICIAL_ANNOUNCEMENT', 'APPROVED', 4, 5, 5, NOW(), 0, 1950, 210, 0, NOW(), NOW());
SET @last_post_id = LAST_INSERT_ID();
INSERT INTO post_attachments (post_id, file_name, file_url, file_type, file_size, source_type, is_deleted, created_at, updated_at)
VALUES (@last_post_id, 'QUY_TRINH_RA_DE_TAI_NCKH_SINH_VIEN_2026.pdf', 'D:\\HK5\\CongNghePhanMem\\tailieuAI\\2026\\CH_quytrinh ra de tai(28.04.2026).pdf', 'PDF', 226578, 'LOCAL_PATH', 0, NOW(), NOW());

-- [6] KẾ HOẠCH ĐĂNG KÝ MÔN HỌC VÀ ĐĂNG KÝ LỚP HỌC LẠI HỌC KỲ 2
INSERT INTO posts (title, content, post_type, status, department_id, author_id, approved_by, approved_at, is_pinned, view_count, like_count, is_deleted, created_at, updated_at)
VALUES ('KẾ HOẠCH ĐĂNG KÝ MÔN HỌC VÀ ĐĂNG KÝ LỚP HỌC LẠI HỌC KỲ 2', '<p>Nhà trường ban hành văn bản chính thức về việc <strong>KẾ HOẠCH ĐĂNG KÝ MÔN HỌC VÀ ĐĂNG KÝ LỚP HỌC LẠI HỌC KỲ 2</strong>.</p>
<p>Sinh viên vui lòng tải hoặc mở tệp PDF đính kèm bên dưới để theo dõi toàn văn chi tiết, nắm rõ các mốc thời gian quan trọng và thực hiện đúng thời hạn quy định.</p>', 'OFFICIAL_ANNOUNCEMENT', 'APPROVED', 3, 29, 29, NOW(), 1, 4850, 612, 0, NOW(), NOW());
SET @last_post_id = LAST_INSERT_ID();
INSERT INTO post_attachments (post_id, file_name, file_url, file_type, file_size, source_type, is_deleted, created_at, updated_at)
VALUES (@last_post_id, 'KE_HOACH_DANG_KY_MON_HOC_HK2_2025_2026.pdf', 'D:\\HK5\\CongNghePhanMem\\tailieuAI\\2026\\TB ĐKMH HK2 2025-2026 - K.ĐTTT.pdf', 'PDF', 1835986, 'LOCAL_PATH', 0, NOW(), NOW());

-- [7] QUY CHẾ THU HỌC PHÍ, MIỄN GIẢM VÀ GIA HẠN THỜI GIAN ĐÓNG HỌC PHÍ SINH VIÊN
INSERT INTO posts (title, content, post_type, status, department_id, author_id, approved_by, approved_at, is_pinned, view_count, like_count, is_deleted, created_at, updated_at)
VALUES ('QUY CHẾ THU HỌC PHÍ, MIỄN GIẢM VÀ GIA HẠN THỜI GIAN ĐÓNG HỌC PHÍ SINH VIÊN', '<p>Nhà trường ban hành văn bản chính thức về việc <strong>QUY CHẾ THU HỌC PHÍ, MIỄN GIẢM VÀ GIA HẠN THỜI GIAN ĐÓNG HỌC PHÍ SINH VIÊN</strong>.</p>
<p>Sinh viên vui lòng tải hoặc mở tệp PDF đính kèm bên dưới để theo dõi toàn văn chi tiết, nắm rõ các mốc thời gian quan trọng và thực hiện đúng thời hạn quy định.</p>', 'OFFICIAL_ANNOUNCEMENT', 'APPROVED', 3, 4, 4, NOW(), 0, 3760, 445, 0, NOW(), NOW());
SET @last_post_id = LAST_INSERT_ID();
INSERT INTO post_attachments (post_id, file_name, file_url, file_type, file_size, source_type, is_deleted, created_at, updated_at)
VALUES (@last_post_id, 'QUY_DINH_THU_HOC_PHI_MIEN_GIAM_2025_2026.pdf', 'D:\\HK5\\CongNghePhanMem\\tailieuAI\\2025\\2810_Quyết định ban hành Quy định thu học phí năm học 2025-2026_1.pdf', 'PDF', 8161321, 'LOCAL_PATH', 0, NOW(), NOW());

-- [8] KẾ HOẠCH VÀ LỊCH BẢO VỆ ĐỒ ÁN TỐT NGHIỆP HỌC KỲ 2 (2025 - 2026)
INSERT INTO posts (title, content, post_type, status, department_id, author_id, approved_by, approved_at, is_pinned, view_count, like_count, is_deleted, created_at, updated_at)
VALUES ('KẾ HOẠCH VÀ LỊCH BẢO VỆ ĐỒ ÁN TỐT NGHIỆP HỌC KỲ 2 (2025 - 2026)', '<p>Nhà trường ban hành văn bản chính thức về việc <strong>KẾ HOẠCH VÀ LỊCH BẢO VỆ ĐỒ ÁN TỐT NGHIỆP HỌC KỲ 2 (2025 - 2026)</strong>.</p>
<p>Sinh viên vui lòng tải hoặc mở tệp PDF đính kèm bên dưới để theo dõi toàn văn chi tiết, nắm rõ các mốc thời gian quan trọng và thực hiện đúng thời hạn quy định.</p>', 'OFFICIAL_ANNOUNCEMENT', 'APPROVED', 7, 29, 29, NOW(), 0, 2320, 298, 0, NOW(), NOW());
SET @last_post_id = LAST_INSERT_ID();
INSERT INTO post_attachments (post_id, file_name, file_url, file_type, file_size, source_type, is_deleted, created_at, updated_at)
VALUES (@last_post_id, 'KE_HOACH_BAO_VE_DO_AN_TOT_NGHIEP_HK2_2025_2026.pdf', 'D:\\HK5\\CongNghePhanMem\\tailieuAI\\2026\\07_Ke Hoach Bao Ve DATN HK 2 2025-2026.signed.pdf', 'PDF', 373259, 'LOCAL_PATH', 0, NOW(), NOW());

-- [9] THÔNG BÁO TIẾP NHẬN HỒ SƠ ĐĂNG KÝ XÉT HỌC BỔNG TÀI TRỢ DOANH NGHIỆP
INSERT INTO posts (title, content, post_type, status, department_id, author_id, approved_by, approved_at, is_pinned, view_count, like_count, is_deleted, created_at, updated_at)
VALUES ('THÔNG BÁO TIẾP NHẬN HỒ SƠ ĐĂNG KÝ XÉT HỌC BỔNG TÀI TRỢ DOANH NGHIỆP', '<p>Nhà trường ban hành văn bản chính thức về việc <strong>THÔNG BÁO TIẾP NHẬN HỒ SƠ ĐĂNG KÝ XÉT HỌC BỔNG TÀI TRỢ DOANH NGHIỆP</strong>.</p>
<p>Sinh viên vui lòng tải hoặc mở tệp PDF đính kèm bên dưới để theo dõi toàn văn chi tiết, nắm rõ các mốc thời gian quan trọng và thực hiện đúng thời hạn quy định.</p>', 'OFFICIAL_ANNOUNCEMENT', 'APPROVED', 3, 4, 4, NOW(), 0, 3100, 395, 0, NOW(), NOW());
SET @last_post_id = LAST_INSERT_ID();
INSERT INTO post_attachments (post_id, file_name, file_url, file_type, file_size, source_type, is_deleted, created_at, updated_at)
VALUES (@last_post_id, 'THONG_BAO_HOC_BONG_TAI_TRO_DOANH_NGHIEP_2025_2026.pdf', 'D:\\HK5\\CongNghePhanMem\\tailieuAI\\2025\\Thông báo học bổng Vallet.pdf', 'PDF', 656824, 'LOCAL_PATH', 0, NOW(), NOW());

-- [10] QUY ĐỊNH ĐÁNH GIÁ ĐIỂM RÈN LUYỆN VÀ TIÊU CHÍ XẾP LOẠI SINH VIÊN THEO KỲ
INSERT INTO posts (title, content, post_type, status, department_id, author_id, approved_by, approved_at, is_pinned, view_count, like_count, is_deleted, created_at, updated_at)
VALUES ('QUY ĐỊNH ĐÁNH GIÁ ĐIỂM RÈN LUYỆN VÀ TIÊU CHÍ XẾP LOẠI SINH VIÊN THEO KỲ', '<p>Nhà trường ban hành văn bản chính thức về việc <strong>QUY ĐỊNH ĐÁNH GIÁ ĐIỂM RÈN LUYỆN VÀ TIÊU CHÍ XẾP LOẠI SINH VIÊN THEO KỲ</strong>.</p>
<p>Sinh viên vui lòng tải hoặc mở tệp PDF đính kèm bên dưới để theo dõi toàn văn chi tiết, nắm rõ các mốc thời gian quan trọng và thực hiện đúng thời hạn quy định.</p>', 'OFFICIAL_ANNOUNCEMENT', 'APPROVED', 1, 6, 6, NOW(), 0, 2890, 340, 0, NOW(), NOW());
SET @last_post_id = LAST_INSERT_ID();
INSERT INTO post_attachments (post_id, file_name, file_url, file_type, file_size, source_type, is_deleted, created_at, updated_at)
VALUES (@last_post_id, 'DANH_GIA_KET_QUA_REN_LUYEN_SINH_VIEN_HK2_2025_2026.pdf', 'D:\\HK5\\CongNghePhanMem\\tailieuAI\\2026\\1987_Thông báo Về việc thực hiện đánh giá kết quả rèn luyện của Sinh viên học kỳ II năm học 2025 - 2026 - Signed (3).pdf', 'PDF', 784240, 'LOCAL_PATH', 0, NOW(), NOW());

-- [11] THÔNG BÁO LỊCH THI KẾT THÚC HỌC PHẦN HỌC KỲ 2 NĂM HỌC 2025 - 2026 (ĐỢT 2)
INSERT INTO posts (title, content, post_type, status, department_id, author_id, approved_by, approved_at, is_pinned, view_count, like_count, is_deleted, created_at, updated_at)
VALUES ('THÔNG BÁO LỊCH THI KẾT THÚC HỌC PHẦN HỌC KỲ 2 NĂM HỌC 2025 - 2026 (ĐỢT 2)', '<p>Nhà trường ban hành văn bản chính thức về việc <strong>THÔNG BÁO LỊCH THI KẾT THÚC HỌC PHẦN HỌC KỲ 2 NĂM HỌC 2025 - 2026 (ĐỢT 2)</strong>.</p>
<p>Sinh viên vui lòng tải hoặc mở tệp PDF đính kèm bên dưới để theo dõi toàn văn chi tiết, nắm rõ các mốc thời gian quan trọng và thực hiện đúng thời hạn quy định.</p>', 'OFFICIAL_ANNOUNCEMENT', 'APPROVED', 3, 29, 29, NOW(), 0, 4410, 520, 0, NOW(), NOW());
SET @last_post_id = LAST_INSERT_ID();
INSERT INTO post_attachments (post_id, file_name, file_url, file_type, file_size, source_type, is_deleted, created_at, updated_at)
VALUES (@last_post_id, 'LICH_THI_KET_THUC_HOC_PHAN_HK2_DOT_2_2025_2026.pdf', 'D:\\HK5\\CongNghePhanMem\\tailieuAI\\2026\\1696_ThongBao_CongBoLichThi_HK2_2025-2026.Dot2 - Signed.pdf', 'PDF', 894015, 'LOCAL_PATH', 0, NOW(), NOW());

-- [12] HƯỚNG DẪN XÉT CÔNG NHẬN TỐT NGHIỆP VÀ LÀM BẰNG ĐẠI HỌC CHÍNH QUY ĐỢT 3
INSERT INTO posts (title, content, post_type, status, department_id, author_id, approved_by, approved_at, is_pinned, view_count, like_count, is_deleted, created_at, updated_at)
VALUES ('HƯỚNG DẪN XÉT CÔNG NHẬN TỐT NGHIỆP VÀ LÀM BẰNG ĐẠI HỌC CHÍNH QUY ĐỢT 3', '<p>Nhà trường ban hành văn bản chính thức về việc <strong>HƯỚNG DẪN XÉT CÔNG NHẬN TỐT NGHIỆP VÀ LÀM BẰNG ĐẠI HỌC CHÍNH QUY ĐỢT 3</strong>.</p>
<p>Sinh viên vui lòng tải hoặc mở tệp PDF đính kèm bên dưới để theo dõi toàn văn chi tiết, nắm rõ các mốc thời gian quan trọng và thực hiện đúng thời hạn quy định.</p>', 'OFFICIAL_ANNOUNCEMENT', 'APPROVED', 3, 4, 4, NOW(), 0, 2750, 310, 0, NOW(), NOW());
SET @last_post_id = LAST_INSERT_ID();
INSERT INTO post_attachments (post_id, file_name, file_url, file_type, file_size, source_type, is_deleted, created_at, updated_at)
VALUES (@last_post_id, 'HUONG_DAN_NOP_LE_PHI_LAM_BANG_TOT_NGHIEP_DOT_3.pdf', 'D:\\HK5\\CongNghePhanMem\\tailieuAI\\2026\\1725_Thông báo nộp lệ phí làm bằng tốt nghiệp đợt 3 năm học 2025-2026 (xét tháng 5-2026) - Signed (2).pdf', 'PDF', 776421, 'LOCAL_PATH', 0, NOW(), NOW());

-- [13] THÔNG BÁO LỊCH THI VÀ ĐĂNG KÝ HỌC PHẦN GIÁO DỤC QUỐC PHÒNG VÀ AN NINH (GDQPAN)
INSERT INTO posts (title, content, post_type, status, department_id, author_id, approved_by, approved_at, is_pinned, view_count, like_count, is_deleted, created_at, updated_at)
VALUES ('THÔNG BÁO LỊCH THI VÀ ĐĂNG KÝ HỌC PHẦN GIÁO DỤC QUỐC PHÒNG VÀ AN NINH (GDQPAN)', '<p>Nhà trường ban hành văn bản chính thức về việc <strong>THÔNG BÁO LỊCH THI VÀ ĐĂNG KÝ HỌC PHẦN GIÁO DỤC QUỐC PHÒNG VÀ AN NINH (GDQPAN)</strong>.</p>
<p>Sinh viên vui lòng tải hoặc mở tệp PDF đính kèm bên dưới để theo dõi toàn văn chi tiết, nắm rõ các mốc thời gian quan trọng và thực hiện đúng thời hạn quy định.</p>', 'OFFICIAL_ANNOUNCEMENT', 'APPROVED', 3, 29, 29, NOW(), 0, 2180, 195, 0, NOW(), NOW());
SET @last_post_id = LAST_INSERT_ID();
INSERT INTO post_attachments (post_id, file_name, file_url, file_type, file_size, source_type, is_deleted, created_at, updated_at)
VALUES (@last_post_id, 'LICH_THI_MON_HOC_GDQPAN_HP1_HP2_HK2_2025_2026.pdf', 'D:\\HK5\\CongNghePhanMem\\tailieuAI\\2026\\2. THÔNG BÁO LỊCH THI HỌC PHẦN 1 VÀ 2 MÔN HỌC GDQPAN HK2 NĂM HỌC 2025 - 2026.pdf', 'PDF', 655447, 'LOCAL_PATH', 0, NOW(), NOW());

-- [14] HƯỚNG DẪN THỦ TỤC XIN XÉT NHẬN ĐIỂM I VÀ HOÃN THI DO LÝ DO BẤT KHẢ KHÁNG
INSERT INTO posts (title, content, post_type, status, department_id, author_id, approved_by, approved_at, is_pinned, view_count, like_count, is_deleted, created_at, updated_at)
VALUES ('HƯỚNG DẪN THỦ TỤC XIN XÉT NHẬN ĐIỂM I VÀ HOÃN THI DO LÝ DO BẤT KHẢ KHÁNG', '<p>Nhà trường ban hành văn bản chính thức về việc <strong>HƯỚNG DẪN THỦ TỤC XIN XÉT NHẬN ĐIỂM I VÀ HOÃN THI DO LÝ DO BẤT KHẢ KHÁNG</strong>.</p>
<p>Sinh viên vui lòng tải hoặc mở tệp PDF đính kèm bên dưới để theo dõi toàn văn chi tiết, nắm rõ các mốc thời gian quan trọng và thực hiện đúng thời hạn quy định.</p>', 'OFFICIAL_ANNOUNCEMENT', 'APPROVED', 3, 4, 4, NOW(), 0, 1670, 142, 0, NOW(), NOW());
SET @last_post_id = LAST_INSERT_ID();
INSERT INTO post_attachments (post_id, file_name, file_url, file_type, file_size, source_type, is_deleted, created_at, updated_at)
VALUES (@last_post_id, 'HUONG_DAN_THU_TUC_NHAN_DIEM_I_HOAN_THI_HK2.pdf', 'D:\\HK5\\CongNghePhanMem\\tailieuAI\\2026\\Thông báo nhận điểm I - đợt 12 HK2.25-26 - Signed (2).pdf', 'PDF', 963386, 'LOCAL_PATH', 0, NOW(), NOW());

-- [15] HƯỚNG DẪN MƯỢN TRẢ LỄ PHỤC VÀ ĐĂNG KÝ DỰ LỄ TỐT NGHIỆP TRỰC TIẾP
INSERT INTO posts (title, content, post_type, status, department_id, author_id, approved_by, approved_at, is_pinned, view_count, like_count, is_deleted, created_at, updated_at)
VALUES ('HƯỚNG DẪN MƯỢN TRẢ LỄ PHỤC VÀ ĐĂNG KÝ DỰ LỄ TỐT NGHIỆP TRỰC TIẾP', '<p>Nhà trường ban hành văn bản chính thức về việc <strong>HƯỚNG DẪN MƯỢN TRẢ LỄ PHỤC VÀ ĐĂNG KÝ DỰ LỄ TỐT NGHIỆP TRỰC TIẾP</strong>.</p>
<p>Sinh viên vui lòng tải hoặc mở tệp PDF đính kèm bên dưới để theo dõi toàn văn chi tiết, nắm rõ các mốc thời gian quan trọng và thực hiện đúng thời hạn quy định.</p>', 'OFFICIAL_ANNOUNCEMENT', 'APPROVED', 1, 6, 6, NOW(), 0, 2930, 365, 0, NOW(), NOW());
SET @last_post_id = LAST_INSERT_ID();
INSERT INTO post_attachments (post_id, file_name, file_url, file_type, file_size, source_type, is_deleted, created_at, updated_at)
VALUES (@last_post_id, 'HUONG_DAN_MUON_TRA_LE_PHUC_TOT_NGHIEP_2026.pdf', 'D:\\HK5\\CongNghePhanMem\\tailieuAI\\2025\\TB.186.2025.Vv_muon_tra_le_phuc_tot_nghiep.pdf', 'PDF', 105798, 'LOCAL_PATH', 0, NOW(), NOW());

-- [16] QUY CHẾ KHEN THƯỞNG SINH VIÊN VÀ ĐOÀN VIÊN ĐẠT THÀNH TÍCH XUẤT SẮC
INSERT INTO posts (title, content, post_type, status, department_id, author_id, approved_by, approved_at, is_pinned, view_count, like_count, is_deleted, created_at, updated_at)
VALUES ('QUY CHẾ KHEN THƯỞNG SINH VIÊN VÀ ĐOÀN VIÊN ĐẠT THÀNH TÍCH XUẤT SẮC', '<p>Nhà trường ban hành văn bản chính thức về việc <strong>QUY CHẾ KHEN THƯỞNG SINH VIÊN VÀ ĐOÀN VIÊN ĐẠT THÀNH TÍCH XUẤT SẮC</strong>.</p>
<p>Sinh viên vui lòng tải hoặc mở tệp PDF đính kèm bên dưới để theo dõi toàn văn chi tiết, nắm rõ các mốc thời gian quan trọng và thực hiện đúng thời hạn quy định.</p>', 'OFFICIAL_ANNOUNCEMENT', 'APPROVED', 1, 6, 6, NOW(), 0, 1580, 188, 0, NOW(), NOW());
SET @last_post_id = LAST_INSERT_ID();
INSERT INTO post_attachments (post_id, file_name, file_url, file_type, file_size, source_type, is_deleted, created_at, updated_at)
VALUES (@last_post_id, 'QUY_CHE_KHEN_THUONG_SINH_VIEN_XUAT_SAC_2026.pdf', 'D:\\HK5\\CongNghePhanMem\\tailieuAI\\2026\\1084 QD_ban hanh quy che khen thuong.pdf', 'PDF', 1232896, 'LOCAL_PATH', 0, NOW(), NOW());

-- [17] HƯỚNG DẪN XÉT CÔNG NHẬN VÀ CHUYỂN ĐỔI TÍN CHỈ HỌC PHẦN TƯƠNG ĐƯƠNG HỌC KỲ 2
INSERT INTO posts (title, content, post_type, status, department_id, author_id, approved_by, approved_at, is_pinned, view_count, like_count, is_deleted, created_at, updated_at)
VALUES ('HƯỚNG DẪN XÉT CÔNG NHẬN VÀ CHUYỂN ĐỔI TÍN CHỈ HỌC PHẦN TƯƠNG ĐƯƠNG HỌC KỲ 2', '<p>Nhà trường ban hành văn bản chính thức về việc <strong>HƯỚNG DẪN XÉT CÔNG NHẬN VÀ CHUYỂN ĐỔI TÍN CHỈ HỌC PHẦN TƯƠNG ĐƯƠNG HỌC KỲ 2</strong>.</p>
<p>Sinh viên vui lòng tải hoặc mở tệp PDF đính kèm bên dưới để theo dõi toàn văn chi tiết, nắm rõ các mốc thời gian quan trọng và thực hiện đúng thời hạn quy định.</p>', 'OFFICIAL_ANNOUNCEMENT', 'APPROVED', 3, 29, 29, NOW(), 0, 2110, 204, 0, NOW(), NOW());
SET @last_post_id = LAST_INSERT_ID();
INSERT INTO post_attachments (post_id, file_name, file_url, file_type, file_size, source_type, is_deleted, created_at, updated_at)
VALUES (@last_post_id, 'CHUYEN_DOI_TIN_CHI_TUONG_DUONG_HK2_2025_2026.pdf', 'D:\\HK5\\CongNghePhanMem\\tailieuAI\\2026\\Thong bao xet cong nhan KQ chuyen doi TC_DTTX HK2 2025 2026.pdf', 'PDF', 759390, 'LOCAL_PATH', 0, NOW(), NOW());

-- [18] THÔNG BÁO CHÍNH SÁCH HỖ TRỢ VÀ MIỄN GIẢM HỌC PHÍ CHO SINH VIÊN KHUYẾT TẬT HỌC KỲ 2
INSERT INTO posts (title, content, post_type, status, department_id, author_id, approved_by, approved_at, is_pinned, view_count, like_count, is_deleted, created_at, updated_at)
VALUES ('THÔNG BÁO CHÍNH SÁCH HỖ TRỢ VÀ MIỄN GIẢM HỌC PHÍ CHO SINH VIÊN KHUYẾT TẬT HỌC KỲ 2', '<p>Nhà trường ban hành văn bản chính thức về việc <strong>THÔNG BÁO CHÍNH SÁCH HỖ TRỢ VÀ MIỄN GIẢM HỌC PHÍ CHO SINH VIÊN KHUYẾT TẬT HỌC KỲ 2</strong>.</p>
<p>Sinh viên vui lòng tải hoặc mở tệp PDF đính kèm bên dưới để theo dõi toàn văn chi tiết, nắm rõ các mốc thời gian quan trọng và thực hiện đúng thời hạn quy định.</p>', 'OFFICIAL_ANNOUNCEMENT', 'APPROVED', 3, 4, 4, NOW(), 0, 1420, 175, 0, NOW(), NOW());
SET @last_post_id = LAST_INSERT_ID();
INSERT INTO post_attachments (post_id, file_name, file_url, file_type, file_size, source_type, is_deleted, created_at, updated_at)
VALUES (@last_post_id, 'CHINH_SACH_HO_TRO_SINH_VIEN_KHUYT_TAT_HK2.pdf', 'D:\\HK5\\CongNghePhanMem\\tailieuAI\\2026\\TB- SV khuyết tật HK2 2025-2026 - Signed (3).pdf', 'PDF', 972297, 'LOCAL_PATH', 0, NOW(), NOW());

-- [19] THÔNG BÁO HOÀN THÀNH NGHĨA VỤ TÀI CHÍNH VÀ XÓA MÔN ĐĂNG KÝ TRỄ HẠN
INSERT INTO posts (title, content, post_type, status, department_id, author_id, approved_by, approved_at, is_pinned, view_count, like_count, is_deleted, created_at, updated_at)
VALUES ('THÔNG BÁO HOÀN THÀNH NGHĨA VỤ TÀI CHÍNH VÀ XÓA MÔN ĐĂNG KÝ TRỄ HẠN', '<p>Nhà trường ban hành văn bản chính thức về việc <strong>THÔNG BÁO HOÀN THÀNH NGHĨA VỤ TÀI CHÍNH VÀ XÓA MÔN ĐĂNG KÝ TRỄ HẠN</strong>.</p>
<p>Sinh viên vui lòng tải hoặc mở tệp PDF đính kèm bên dưới để theo dõi toàn văn chi tiết, nắm rõ các mốc thời gian quan trọng và thực hiện đúng thời hạn quy định.</p>', 'OFFICIAL_ANNOUNCEMENT', 'APPROVED', 3, 4, 4, NOW(), 0, 3980, 472, 0, NOW(), NOW());
SET @last_post_id = LAST_INSERT_ID();
INSERT INTO post_attachments (post_id, file_name, file_url, file_type, file_size, source_type, is_deleted, created_at, updated_at)
VALUES (@last_post_id, 'THONG_BAO_NGHIA_VU_TAI_CHINH_HOC_PHI_SO_24.pdf', 'D:\\HK5\\CongNghePhanMem\\tailieuAI\\2026\\Thông báo số 24_ V.v hoàn thành nghĩa vụ nộp học phí ...signed.pdf', 'PDF', 242476, 'LOCAL_PATH', 0, NOW(), NOW());

-- [20] KẾ HOẠCH TỔ CHỨC ĐỐI THOẠI TRỰC TIẾP GIỮA BAN GIÁM HIỆU VÀ SINH VIÊN HỌC KỲ 2
INSERT INTO posts (title, content, post_type, status, department_id, author_id, approved_by, approved_at, is_pinned, view_count, like_count, is_deleted, created_at, updated_at)
VALUES ('KẾ HOẠCH TỔ CHỨC ĐỐI THOẠI TRỰC TIẾP GIỮA BAN GIÁM HIỆU VÀ SINH VIÊN HỌC KỲ 2', '<p>Nhà trường ban hành văn bản chính thức về việc <strong>KẾ HOẠCH TỔ CHỨC ĐỐI THOẠI TRỰC TIẾP GIỮA BAN GIÁM HIỆU VÀ SINH VIÊN HỌC KỲ 2</strong>.</p>
<p>Sinh viên vui lòng tải hoặc mở tệp PDF đính kèm bên dưới để theo dõi toàn văn chi tiết, nắm rõ các mốc thời gian quan trọng và thực hiện đúng thời hạn quy định.</p>', 'OFFICIAL_ANNOUNCEMENT', 'APPROVED', 3, 2, 2, NOW(), 0, 2560, 315, 0, NOW(), NOW());
SET @last_post_id = LAST_INSERT_ID();
INSERT INTO post_attachments (post_id, file_name, file_url, file_type, file_size, source_type, is_deleted, created_at, updated_at)
VALUES (@last_post_id, 'KE_HOACH_DOI_THOAI_SINH_VIEN_HK2_2025_2026.pdf', 'D:\\HK5\\CongNghePhanMem\\tailieuAI\\2026\\KH đối thoại sinh viên HKII. 25-26.pdf', 'PDF', 227538, 'LOCAL_PATH', 0, NOW(), NOW());

