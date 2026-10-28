-- ==============================================================================
-- DATABASE SCHEMA DDL FOR QAUTE PORTAL (SPRING BOOT 3.3 + MYSQL 8 / TIDB CLOUD)
-- Phiên bản: 2.0.0-UNIFIED (Đồng bộ 100% với 23 JPA Entities hiện tại của hệ thống)
-- Bộ mã ký tự: utf8mb4 | Quy tắc đối chiếu: utf8mb4_unicode_ci
-- Kiến trúc: 3NF Normalized, SLA Engine, RBAC, Dual-Feed Moderation, AI RAG & Academic Sync
-- ==============================================================================

SET FOREIGN_KEY_CHECKS = 0;

-- 1. Hệ Thống Học Vụ & Đồng Bộ (Academic Integration)
DROP TABLE IF EXISTS academic_sync_histories;
DROP TABLE IF EXISTS academic_curriculum_subjects;
DROP TABLE IF EXISTS academic_course_schedules;
DROP TABLE IF EXISTS academic_student_grades;
DROP TABLE IF EXISTS academic_student_profiles;

-- 2. Quản Trị & Giao Tiếp Nội Bộ (Admin & Internal)
DROP TABLE IF EXISTS internal_staff_admin_messages;

-- 3. Phân Hệ Trợ Lý AI RAG & Tri Thức (AI Assistant & FAQs)
DROP TABLE IF EXISTS ai_message_feedbacks;
DROP TABLE IF EXISTS ai_chat_messages;
DROP TABLE IF EXISTS ai_conversations;
DROP TABLE IF EXISTS faqs;

-- 4. Bảng Tin & Diễn Đàn Sinh Viên (Social Feed & Moderation)
DROP TABLE IF EXISTS post_reports;
DROP TABLE IF EXISTS likes;
DROP TABLE IF EXISTS post_comments;
DROP TABLE IF EXISTS post_attachments;
DROP TABLE IF EXISTS posts;

-- 5. Quy Trình Hỗ Trợ Ticket & SLA (Ticket & SLA Lifecycle)
DROP TABLE IF EXISTS attachments;
DROP TABLE IF EXISTS ticket_transfer_histories;
DROP TABLE IF EXISTS ticket_histories;
DROP TABLE IF EXISTS tickets;

-- 6. Xác Thực & Phân Quyền (Auth & RBAC)
DROP TABLE IF EXISTS otp_tokens;
DROP TABLE IF EXISTS users;
DROP TABLE IF EXISTS departments;
DROP TABLE IF EXISTS roles;

SET FOREIGN_KEY_CHECKS = 1;

-- ==============================================================================
-- PHẦN 1: TÀI KHOẢN, PHÂN QUYỀN & TỔ CHỨC (AUTH & ORGANIZATIONAL UNITS)
-- ==============================================================================

-- ------------------------------------------------------------------------------
-- 1. BẢNG ROLES (Vai trò người dùng trong hệ thống RBAC)
-- ------------------------------------------------------------------------------
CREATE TABLE roles (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE COMMENT 'Tên quyền hạn: ROLE_STUDENT, ROLE_STAFF, ROLE_ADMIN',
    description VARCHAR(255) NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by VARCHAR(100) DEFAULT 'SYSTEM',
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ------------------------------------------------------------------------------
-- 2. BẢNG DEPARTMENTS (Khoa chuyên ngành & Phòng ban chức năng HCMUTE)
-- ------------------------------------------------------------------------------
CREATE TABLE departments (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(150) NOT NULL UNIQUE COMMENT 'Tên đầy đủ của Khoa/Phòng',
    code VARCHAR(50) NOT NULL UNIQUE COMMENT 'Mã định danh duy nhất: DOAN_HOI, TUYEN_SINH, DAO_TAO, KHOA_CNTT, KHOA_NN...',
    office_location VARCHAR(100) NULL COMMENT 'Văn phòng làm việc (vd: A1-102, E1-402)',
    contact_email VARCHAR(100) NULL,
    contact_phone VARCHAR(50) NULL,
    description TEXT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by VARCHAR(100) DEFAULT 'SYSTEM',
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ------------------------------------------------------------------------------
-- 3. BẢNG USERS (Người dùng: Sinh viên, Cán bộ, Quản trị viên)
-- ------------------------------------------------------------------------------
CREATE TABLE users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(100) NOT NULL UNIQUE COMMENT 'Tên đăng nhập hoặc MSSV',
    password_hash VARCHAR(255) NOT NULL COMMENT 'Mật khẩu băm an toàn theo chuẩn BCrypt',
    full_name VARCHAR(150) NOT NULL,
    email VARCHAR(150) NOT NULL UNIQUE,
    phone VARCHAR(30) NULL,
    avatar_url VARCHAR(500) NULL,
    portal_username VARCHAR(100) NULL COMMENT 'Tài khoản đăng nhập cổng học vụ online.hcmute.edu.vn',
    portal_password_enc VARCHAR(500) NULL COMMENT 'Mật khẩu portal mã hóa phục vụ crawler đồng bộ',
    role_id BIGINT NOT NULL,
    department_id BIGINT NULL COMMENT 'Gắn với Khoa/Phòng ban nếu là Cán bộ (ROLE_STAFF)',
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE, PENDING_ACTIVATION, LOCKED',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by VARCHAR(100) DEFAULT 'SYSTEM',
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT fk_users_role FOREIGN KEY (role_id) REFERENCES roles(id),
    CONSTRAINT fk_users_department FOREIGN KEY (department_id) REFERENCES departments(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ------------------------------------------------------------------------------
-- 4. BẢNG OTP_TOKENS (Mã xác thực gửi qua Email kích hoạt & quên mật khẩu)
-- ------------------------------------------------------------------------------
CREATE TABLE otp_tokens (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    email VARCHAR(150) NOT NULL,
    otp_code VARCHAR(10) NOT NULL,
    purpose VARCHAR(50) NOT NULL COMMENT 'Mục đích: REGISTRATION, PASSWORD_RESET',
    expired_at TIMESTAMP NOT NULL,
    is_used BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ==============================================================================
-- PHẦN 2: QUY TRÌNH QUẢN LÝ TICKET HỌC VỤ & CAM KẾT SLA (TICKET ENGINE)
-- ==============================================================================

-- ------------------------------------------------------------------------------
-- 5. BẢNG TICKETS (Yêu cầu tư vấn, giải quyết khiếu nại học vụ)
-- ------------------------------------------------------------------------------
CREATE TABLE tickets (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    ticket_code VARCHAR(50) NOT NULL UNIQUE COMMENT 'Mã vé tra cứu: TK-YYYYMMDD-XXXX',
    title VARCHAR(255) NOT NULL,
    description TEXT NOT NULL,
    department_id BIGINT NOT NULL COMMENT 'Đơn vị phụ trách xử lý',
    creator_id BIGINT NULL COMMENT 'Người tạo vé (NULL nếu Khách vãng lai gửi)',
    guest_name VARCHAR(150) NULL,
    guest_email VARCHAR(150) NULL,
    guest_token VARCHAR(100) NULL COMMENT 'Token bí mật để Guest theo dõi tiến độ qua URL',
    assigned_to BIGINT NULL COMMENT 'Cán bộ đang tiếp nhận giải quyết',
    priority VARCHAR(20) NOT NULL DEFAULT 'MEDIUM' COMMENT 'Mức độ ưu tiên: LOW, MEDIUM, HIGH, URGENT',
    topic_category VARCHAR(150) NULL COMMENT 'Danh mục thủ tục học vụ (SLA tự động từ 24h - 120h)',
    status VARCHAR(30) NOT NULL DEFAULT 'OPEN' COMMENT 'OPEN, IN_PROGRESS, WAITING_STUDENT, RESOLVED, CLOSED, OVERDUE',
    due_date TIMESTAMP NOT NULL COMMENT 'Hạn chót cam kết giải quyết theo chuẩn SLA',
    resolved_at TIMESTAMP NULL,
    closed_at TIMESTAMP NULL,
    rating INT NULL COMMENT 'Điểm đánh giá hài lòng CSAT (1-5 sao)',
    has_student_reply BOOLEAN NOT NULL DEFAULT FALSE COMMENT 'Cờ cảnh báo realtime có tin nhắn mới từ Sinh viên',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by VARCHAR(100) DEFAULT 'SYSTEM',
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT fk_tickets_department FOREIGN KEY (department_id) REFERENCES departments(id),
    CONSTRAINT fk_tickets_creator FOREIGN KEY (creator_id) REFERENCES users(id),
    CONSTRAINT fk_tickets_assigned_to FOREIGN KEY (assigned_to) REFERENCES users(id),
    INDEX idx_tickets_dept_status_due (department_id, status, due_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ------------------------------------------------------------------------------
-- 6. BẢNG TICKET_HISTORIES (Nhật ký trạng thái & Thảo luận tương tác trong Ticket)
-- ------------------------------------------------------------------------------
CREATE TABLE ticket_histories (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    ticket_id BIGINT NOT NULL,
    actor_id BIGINT NULL COMMENT 'Người thực hiện thao tác/gửi tin nhắn (NULL nếu là Guest)',
    actor_name VARCHAR(150) NOT NULL,
    from_status VARCHAR(30) NULL,
    to_status VARCHAR(30) NOT NULL,
    action_note TEXT NULL COMMENT 'Nội dung phản hồi hoặc ghi chú nghiệp vụ',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by VARCHAR(100) DEFAULT 'SYSTEM',
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT fk_histories_ticket FOREIGN KEY (ticket_id) REFERENCES tickets(id) ON DELETE CASCADE,
    CONSTRAINT fk_histories_actor FOREIGN KEY (actor_id) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ------------------------------------------------------------------------------
-- 7. BẢNG TICKET_TRANSFER_HISTORIES (Lịch sử chuyển giao Ticket giữa các Khoa/Phòng)
-- ------------------------------------------------------------------------------
CREATE TABLE ticket_transfer_histories (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    ticket_id BIGINT NOT NULL,
    from_department_id BIGINT NOT NULL,
    to_department_id BIGINT NOT NULL,
    transferred_by_user_id BIGINT NOT NULL,
    reason VARCHAR(500) NOT NULL COMMENT 'Lý do chuyển giao thẩm quyền xử lý',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by VARCHAR(100) DEFAULT 'SYSTEM',
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT fk_tth_ticket FOREIGN KEY (ticket_id) REFERENCES tickets(id) ON DELETE CASCADE,
    CONSTRAINT fk_tth_from_dept FOREIGN KEY (from_department_id) REFERENCES departments(id),
    CONSTRAINT fk_tth_to_dept FOREIGN KEY (to_department_id) REFERENCES departments(id),
    CONSTRAINT fk_tth_user FOREIGN KEY (transferred_by_user_id) REFERENCES users(id),
    INDEX idx_tth_ticket_id (ticket_id),
    INDEX idx_tth_from_dept (from_department_id),
    INDEX idx_tth_to_dept (to_department_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ------------------------------------------------------------------------------
-- 8. BẢNG ATTACHMENTS (Tệp đính kèm Ticket: PDF đơn từ, ảnh minh chứng)
-- ------------------------------------------------------------------------------
CREATE TABLE attachments (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    ticket_id BIGINT NULL,
    ticket_history_id BIGINT NULL,
    file_name VARCHAR(255) NOT NULL,
    file_url VARCHAR(500) NOT NULL,
    file_type VARCHAR(50) NOT NULL COMMENT 'IMAGE, PDF, DOCX, XLSX...',
    file_size BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by VARCHAR(100) DEFAULT 'SYSTEM',
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT fk_attachments_ticket FOREIGN KEY (ticket_id) REFERENCES tickets(id) ON DELETE CASCADE,
    CONSTRAINT fk_attachments_history FOREIGN KEY (ticket_history_id) REFERENCES ticket_histories(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ==============================================================================
-- PHẦN 3: BẢNG TIN CHÍNH THỨC & DIỄN ĐÀN SINH VIÊN (FEED & MODERATION)
-- ==============================================================================

-- ------------------------------------------------------------------------------
-- 9. BẢNG POSTS (Thông báo chính thức & Bài viết diễn đàn cộng đồng)
-- ------------------------------------------------------------------------------
CREATE TABLE posts (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    content LONGTEXT NOT NULL,
    post_type VARCHAR(30) NOT NULL DEFAULT 'OFFICIAL_ANNOUNCEMENT' COMMENT 'OFFICIAL_ANNOUNCEMENT hoặc STUDENT_FORUM',
    department_id BIGINT NULL COMMENT 'Đơn vị ban hành thông báo (nếu là bài chính thức)',
    author_id BIGINT NULL COMMENT 'Tác giả bài viết',
    status VARCHAR(30) NOT NULL DEFAULT 'APPROVED' COMMENT 'PENDING_APPROVAL, APPROVED, REJECTED, HIDDEN',
    rejection_reason VARCHAR(255) NULL,
    approved_by BIGINT NULL COMMENT 'Cán bộ phê duyệt bài viết diễn đàn',
    approved_at TIMESTAMP NULL,
    is_pinned BOOLEAN NOT NULL DEFAULT FALSE COMMENT 'Ghim thông báo lên đầu trang',
    view_count INT NOT NULL DEFAULT 0,
    video_status VARCHAR(20) DEFAULT 'NONE' COMMENT 'NONE, PROCESSING, COMPLETED, FAILED',
    video_url VARCHAR(500) NULL,
    like_count INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by VARCHAR(100) DEFAULT 'SYSTEM',
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT fk_posts_department FOREIGN KEY (department_id) REFERENCES departments(id),
    CONSTRAINT fk_posts_author FOREIGN KEY (author_id) REFERENCES users(id),
    CONSTRAINT fk_posts_approver FOREIGN KEY (approved_by) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ------------------------------------------------------------------------------
-- 10. BẢNG POST_ATTACHMENTS (Văn bản công văn đính kèm thông báo & video bài viết)
-- ------------------------------------------------------------------------------
CREATE TABLE post_attachments (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    post_id BIGINT NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    file_url VARCHAR(500) NOT NULL,
    file_type VARCHAR(50) NOT NULL COMMENT 'PDF, DOCX, XLSX, MP4, IMAGE',
    file_size BIGINT NOT NULL DEFAULT 0,
    source_type VARCHAR(30) DEFAULT 'DIRECT_UPLOAD' COMMENT 'DIRECT_UPLOAD hoặc NODEJS_WEBHOOK',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by VARCHAR(100) DEFAULT 'SYSTEM',
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT fk_post_attachments_post FOREIGN KEY (post_id) REFERENCES posts(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ------------------------------------------------------------------------------
-- 11. BẢNG POST_COMMENTS (Bình luận & Phản hồi phân cấp nhiều tầng)
-- ------------------------------------------------------------------------------
CREATE TABLE post_comments (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    post_id BIGINT NOT NULL,
    author_id BIGINT NOT NULL,
    parent_id BIGINT NULL COMMENT 'ID bình luận cha (Threaded/Nested Comments)',
    content VARCHAR(1000) NOT NULL,
    like_count INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by VARCHAR(100) DEFAULT 'SYSTEM',
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT fk_comments_post FOREIGN KEY (post_id) REFERENCES posts(id) ON DELETE CASCADE,
    CONSTRAINT fk_comments_author FOREIGN KEY (author_id) REFERENCES users(id),
    CONSTRAINT fk_comments_parent FOREIGN KEY (parent_id) REFERENCES post_comments(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ------------------------------------------------------------------------------
-- 12. BẢNG LIKES (Lượt thích đa hình cho Bài viết và Bình luận)
-- ------------------------------------------------------------------------------
CREATE TABLE likes (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    target_type VARCHAR(20) NOT NULL COMMENT 'POST hoặc COMMENT',
    target_id BIGINT NOT NULL COMMENT 'ID của Bài viết hoặc Bình luận được thích',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by VARCHAR(100) DEFAULT 'SYSTEM',
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT fk_likes_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT uk_user_target_like UNIQUE (user_id, target_type, target_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ------------------------------------------------------------------------------
-- 13. BẢNG POST_REPORTS (Báo cáo vi phạm nội dung diễn đàn sinh viên)
-- ------------------------------------------------------------------------------
CREATE TABLE post_reports (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    post_id BIGINT NULL,
    reporter_id BIGINT NULL,
    reason VARCHAR(50) NOT NULL COMMENT 'SPAM, INAPPROPRIATE_LANGUAGE, WRONG_ACADEMIC_INFO, OTHER',
    details VARCHAR(500) NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING, RESOLVED, DISMISSED',
    resolved_by BIGINT NULL COMMENT 'Cán bộ kiểm duyệt tiếp nhận xử lý',
    resolved_at TIMESTAMP NULL,
    action_taken VARCHAR(50) NULL COMMENT 'Biện pháp: HIDE_POST, DISMISS, LOCK_USER',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by VARCHAR(100) DEFAULT 'SYSTEM',
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT fk_reports_post FOREIGN KEY (post_id) REFERENCES posts(id) ON DELETE CASCADE,
    CONSTRAINT fk_reports_reporter FOREIGN KEY (reporter_id) REFERENCES users(id),
    CONSTRAINT fk_reports_resolver FOREIGN KEY (resolved_by) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ==============================================================================
-- PHẦN 4: HỎI ĐÁP FAQ & TRỢ LÝ AI RAG (FAQ & CONVERSATION STORAGE)
-- ==============================================================================

-- ------------------------------------------------------------------------------
-- 14. BẢNG FAQS (Kho câu hỏi thường gặp đã được Cán bộ kiểm chứng)
-- ------------------------------------------------------------------------------
CREATE TABLE faqs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    question VARCHAR(500) NOT NULL,
    answer TEXT NOT NULL,
    department_id BIGINT NOT NULL,
    category VARCHAR(100) NULL,
    keywords VARCHAR(255) NULL,
    view_count INT NOT NULL DEFAULT 0,
    post_date TIMESTAMP NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by VARCHAR(100) DEFAULT 'SYSTEM',
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT fk_faqs_department FOREIGN KEY (department_id) REFERENCES departments(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ------------------------------------------------------------------------------
-- 15. BẢNG AI_CONVERSATIONS (Phiên trò chuyện trợ lý ảo AI RAG)
-- ------------------------------------------------------------------------------
CREATE TABLE ai_conversations (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    session_id VARCHAR(36) NOT NULL UNIQUE COMMENT 'UUID định danh phiên hội thoại',
    title VARCHAR(255) NULL COMMENT 'Tiêu đề tự động tóm tắt từ câu hỏi đầu',
    user_id BIGINT NULL COMMENT 'Người dùng tương tác (NULL nếu là Guest)',
    guest_email VARCHAR(150) NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE, CLOSED, TRANSFERRED_TO_TICKET',
    message_count INT NOT NULL DEFAULT 0,
    last_message_at TIMESTAMP NULL,
    total_tokens_used INT NOT NULL DEFAULT 0,
    ticket_id BIGINT NULL COMMENT 'ID của Ticket nếu được chuyển thành Ticket',
    metadata TEXT NULL COMMENT 'JSON metadata bổ sung',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by VARCHAR(100) DEFAULT 'SYSTEM',
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT fk_conv_user FOREIGN KEY (user_id) REFERENCES users(id),
    INDEX idx_conv_session_id (session_id),
    INDEX idx_conv_user_last_msg (user_id, last_message_at),
    INDEX idx_conv_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ------------------------------------------------------------------------------
-- 16. BẢNG AI_CHAT_MESSAGES (Lịch sử câu hỏi & Câu trả lời trợ lý AI)
-- ------------------------------------------------------------------------------
CREATE TABLE ai_chat_messages (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    conversation_id BIGINT NOT NULL,
    role VARCHAR(20) NOT NULL COMMENT 'USER hoặc ASSISTANT',
    content TEXT NOT NULL,
    sources_json TEXT NULL COMMENT 'Danh sách tài liệu / FAQ trích dẫn nguồn JSON',
    provider VARCHAR(50) NULL COMMENT 'GEMINI, DIFY, LOCAL_RAG',
    confidence DOUBLE NULL COMMENT 'Độ tin cậy của câu trả lời (0.0 - 1.0)',
    tokens_used INT NULL DEFAULT 0,
    response_time_ms BIGINT NULL,
    is_flagged BOOLEAN NOT NULL DEFAULT FALSE,
    flag_reason VARCHAR(500) NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by VARCHAR(100) DEFAULT 'SYSTEM',
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT fk_msg_conversation FOREIGN KEY (conversation_id) REFERENCES ai_conversations(id) ON DELETE CASCADE,
    INDEX idx_msg_conv_created (conversation_id, created_at),
    INDEX idx_msg_role (role)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ------------------------------------------------------------------------------
-- 17. BẢNG AI_MESSAGE_FEEDBACKS (Phản hồi đánh giá chất lượng câu trả lời AI)
-- ------------------------------------------------------------------------------
CREATE TABLE ai_message_feedbacks (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    message_id BIGINT NOT NULL,
    feedback_type VARCHAR(30) NOT NULL COMMENT 'HELPFUL, UNHELPFUL, INACCURATE, IRRELEVANT...',
    comment VARCHAR(1000) NULL,
    user_id BIGINT NULL,
    guest_email VARCHAR(150) NULL,
    rating INT NULL COMMENT 'Điểm đánh giá (1-5 sao)',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by VARCHAR(100) DEFAULT 'SYSTEM',
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT fk_feedback_msg FOREIGN KEY (message_id) REFERENCES ai_chat_messages(id) ON DELETE CASCADE,
    CONSTRAINT fk_feedback_user FOREIGN KEY (user_id) REFERENCES users(id),
    INDEX idx_feedback_msg (message_id),
    INDEX idx_feedback_user (user_id),
    INDEX idx_feedback_type (feedback_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ==============================================================================
-- PHẦN 5: THÔNG BÁO & TIN NHẮN NỘI BỘ QUẢN TRỊ (ADMIN & INTERNAL)
-- ==============================================================================

-- ------------------------------------------------------------------------------
-- 18. BẢNG INTERNAL_STAFF_ADMIN_MESSAGES (Giao tiếp nội bộ Cán bộ - Admin)
-- ------------------------------------------------------------------------------
CREATE TABLE internal_staff_admin_messages (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    sender_id BIGINT NOT NULL,
    receiver_id BIGINT NULL COMMENT 'Người nhận đích danh (NULL nếu gửi chung toàn bộ đơn vị)',
    department_id BIGINT NULL,
    subject VARCHAR(255) NULL,
    content TEXT NOT NULL,
    message_type VARCHAR(50) NOT NULL DEFAULT 'INQUIRY' COMMENT 'INQUIRY, ANNOUNCEMENT, SYSTEM_ALERT...',
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by VARCHAR(100) DEFAULT 'SYSTEM',
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT fk_internal_msg_sender FOREIGN KEY (sender_id) REFERENCES users(id),
    CONSTRAINT fk_internal_msg_receiver FOREIGN KEY (receiver_id) REFERENCES users(id),
    CONSTRAINT fk_internal_msg_department FOREIGN KEY (department_id) REFERENCES departments(id),
    INDEX idx_internal_msg_dept_created (department_id, created_at),
    INDEX idx_internal_msg_sender_created (sender_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ==============================================================================
-- PHẦN 6: DỮ LIỆU HỌC VỤ SỐ ĐỒNG BỘ TỪ CỔNG TRƯỜNG (ACADEMIC CRAWLER)
-- ==============================================================================

-- ------------------------------------------------------------------------------
-- 19. BẢNG ACADEMIC_STUDENT_PROFILES (Hồ sơ học vụ chính thức của Sinh viên)
-- ------------------------------------------------------------------------------
CREATE TABLE academic_student_profiles (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    student_id VARCHAR(50) NOT NULL UNIQUE COMMENT 'Mã số sinh viên (MSSV)',
    full_name VARCHAR(150) NULL,
    gender VARCHAR(20) NULL,
    birthday VARCHAR(30) NULL,
    birth_place VARCHAR(100) NULL,
    student_class VARCHAR(50) NULL COMMENT 'Lớp sinh hoạt',
    faculty VARCHAR(150) NULL COMMENT 'Khoa chủ quản',
    major VARCHAR(150) NULL COMMENT 'Chuyên ngành đào tạo',
    academic_year VARCHAR(50) NULL COMMENT 'Niên khóa đào tạo',
    education_type VARCHAR(50) NULL COMMENT 'Chất lượng cao, Đại trà...',
    study_program VARCHAR(150) NULL,
    academic_status VARCHAR(50) NULL COMMENT 'Đang học, Nghỉ học tạm thời...',
    academic_advisor VARCHAR(150) NULL COMMENT 'Cố vấn học tập phụ trách',
    email VARCHAR(100) NULL,
    phone VARCHAR(30) NULL,
    citizen_id VARCHAR(30) NULL,
    address VARCHAR(255) NULL,
    avatar_url VARCHAR(500) NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by VARCHAR(100) DEFAULT 'SYSTEM',
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ------------------------------------------------------------------------------
-- 20. BẢNG ACADEMIC_STUDENT_GRADES (Bảng điểm tích lũy & Điểm học phần)
-- ------------------------------------------------------------------------------
CREATE TABLE academic_student_grades (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    student_id VARCHAR(50) NOT NULL,
    course_code VARCHAR(50) NOT NULL,
    course_name VARCHAR(255) NOT NULL,
    credits INT NULL,
    midterm_score DOUBLE NULL,
    final_score DOUBLE NULL,
    total_score DOUBLE NULL,
    grade_letter VARCHAR(10) NULL COMMENT 'A+, A, B+, B, C, D, F',
    semester VARCHAR(100) NULL COMMENT 'Học kỳ (vd: HK1_2026_2027)',
    is_passed BOOLEAN NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by VARCHAR(100) DEFAULT 'SYSTEM',
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    INDEX idx_grades_student (student_id),
    INDEX idx_grades_semester (semester)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ------------------------------------------------------------------------------
-- 21. BẢNG ACADEMIC_COURSE_SCHEDULES (Thời khóa biểu theo tuần & tiết học)
-- ------------------------------------------------------------------------------
CREATE TABLE academic_course_schedules (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    student_id VARCHAR(50) NOT NULL,
    course_code VARCHAR(50) NOT NULL,
    course_name VARCHAR(255) NOT NULL,
    day_of_week VARCHAR(30) NULL COMMENT 'Thứ 2, Thứ 3, Thứ 4...',
    shift VARCHAR(50) NULL COMMENT 'Tiết 1-3, Tiết 7-9...',
    start_time VARCHAR(10) NULL COMMENT 'HH:mm (vd: 07:30)',
    end_time VARCHAR(10) NULL COMMENT 'HH:mm (vd: 09:15)',
    room VARCHAR(100) NULL COMMENT 'Phòng học: A1-302, E1-401...',
    lecturer VARCHAR(150) NULL,
    semester VARCHAR(100) NULL,
    group_code VARCHAR(30) NULL,
    credits INT NULL,
    phase VARCHAR(30) NULL COMMENT 'Đợt 1, Đợt 2, Cả học kỳ',
    weeks VARCHAR(100) NULL COMMENT 'Dãy tuần học (vd: 1-10, 12-18)',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by VARCHAR(100) DEFAULT 'SYSTEM',
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    INDEX idx_schedules_student (student_id),
    INDEX idx_schedules_day (day_of_week)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ------------------------------------------------------------------------------
-- 22. BẢNG ACADEMIC_CURRICULUM_SUBJECTS (Tiến độ chương trình khung đào tạo)
-- ------------------------------------------------------------------------------
CREATE TABLE academic_curriculum_subjects (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    student_id VARCHAR(50) NOT NULL,
    course_code VARCHAR(50) NOT NULL,
    course_name VARCHAR(255) NOT NULL,
    credits INT NULL,
    is_completed BOOLEAN NULL,
    semester_recommended INT NULL COMMENT 'Học kỳ khuyến nghị trong lộ trình',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by VARCHAR(100) DEFAULT 'SYSTEM',
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    INDEX idx_curriculum_student (student_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ------------------------------------------------------------------------------
-- 23. BẢNG ACADEMIC_SYNC_HISTORIES (Nhật ký phiên đồng bộ dữ liệu cổng trường)
-- ------------------------------------------------------------------------------
CREATE TABLE academic_sync_histories (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    student_id VARCHAR(20) NOT NULL,
    status VARCHAR(30) NULL COMMENT 'SUCCESS, FAILED, DEMO_FALLBACK',
    message VARCHAR(255) NULL,
    grades_count INT NULL DEFAULT 0,
    schedules_count INT NULL DEFAULT 0,
    curriculums_count INT NULL DEFAULT 0,
    synced_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by VARCHAR(100) DEFAULT 'SYSTEM',
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    INDEX idx_sync_student (student_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ==============================================================================
-- DỮ LIỆU KHỞI TẠO HỆ THỐNG MẪU (SEED DATA - MẬT KHẨU MẪU: 123456)
-- ==============================================================================

-- 1. Khởi tạo Vai trò (Roles)
INSERT INTO roles (id, name, description, created_by) VALUES
(1, 'ROLE_STUDENT', 'Sinh viên trường ĐH Sư phạm Kỹ thuật TP.HCM', 'SYSTEM'),
(2, 'ROLE_STAFF', 'Cán bộ / Giảng viên tư vấn các Khoa/Phòng', 'SYSTEM'),
(3, 'ROLE_ADMIN', 'Quản trị viên toàn quyền hệ thống', 'SYSTEM')
ON DUPLICATE KEY UPDATE name=VALUES(name);

-- 2. Khởi tạo Khoa & Phòng ban (Departments)
INSERT INTO departments (id, name, code, office_location, contact_email, contact_phone, description, created_by) VALUES
(1, 'Đoàn Thanh niên - Hội Sinh viên', 'DOAN_HOI', 'Phòng A1-102', 'doantn@hcmute.edu.vn', '028.3722.1223', 'Phong trào sinh viên, tình nguyện, rèn luyện, truyền thông', 'SYSTEM'),
(2, 'Phòng Tuyển sinh & Truyền thông', 'TUYEN_SINH', 'Phòng A1-101', 'tuyensinh@hcmute.edu.vn', '028.3722.5766', 'Tư vấn tuyển sinh các hệ, đề án tuyển sinh', 'SYSTEM'),
(3, 'Phòng Đào tạo & Công tác Sinh viên', 'DAO_TAO', 'Phòng A1-201', 'daotao@hcmute.edu.vn', '028.3896.8641', 'Học vụ, điểm số, đăng ký môn học, chứng chỉ ngoại ngữ TOEIC, học bổng', 'SYSTEM'),
(4, 'Khoa Công nghệ Thông tin', 'KHOA_CNTT', 'Tòa nhà E1-402', 'cntt@hcmute.edu.vn', '028.3897.2092', 'Đồ án tốt nghiệp, học phần chuyên ngành CNTT, thực tập doanh nghiệp', 'SYSTEM'),
(5, 'Khoa Ngoại ngữ', 'KHOA_NN', 'Tòa nhà A1-306', 'nn@hcmute.edu.vn', '028.3896.1373', 'Chuyển điểm chuẩn đầu ra tiếng Anh, kỳ thi ĐGNLTA đầu vào', 'SYSTEM'),
(6, 'Khoa Cơ khí Chế tạo máy', 'KHOA_CKM', 'Tòa nhà A1-401', 'ckm@hcmute.edu.vn', '028.3896.8642', 'Đồ án tốt nghiệp CKM, Cơ điện tử, Kỹ thuật Công nghiệp', 'SYSTEM'),
(7, 'Khoa Điện - Điện tử', 'KHOA_DIEN', 'Tòa nhà B1-201', 'feee@hcmute.edu.vn', '028.3896.8643', 'Tự động hóa, Viễn thông, IoT, Điện tử công nghiệp', 'SYSTEM'),
(8, 'Khoa Kinh tế', 'KHOA_KT', 'Tòa nhà A1-302', 'kinhte@hcmute.edu.vn', '028.3896.8644', 'Kế toán, Quản trị kinh doanh, Thương mại điện tử, Logistics', 'SYSTEM')
ON DUPLICATE KEY UPDATE name=VALUES(name);

-- 3. Khởi tạo Tài khoản mẫu (Tất cả mật khẩu mặc định: 123456)
-- Hash BCrypt cho '123456': $2a$10$EblZqNptyYvcLm/VwDCVAuBjzZOI7khzdyGPBr08PpIi0na624b8.
INSERT INTO users (id, username, password_hash, full_name, email, role_id, department_id, status, created_by) VALUES
(1, 'admin', '$2a$10$EblZqNptyYvcLm/VwDCVAuBjzZOI7khzdyGPBr08PpIi0na624b8.', 'Quản Trị Viên Hệ Thống', 'admin@hcmute.edu.vn', 3, NULL, 'ACTIVE', 'SYSTEM'),
(2, 'admin_main', '$2a$10$EblZqNptyYvcLm/VwDCVAuBjzZOI7khzdyGPBr08PpIi0na624b8.', 'Admin Quản Trị Cấp Cao', 'admin@gmail.com', 3, NULL, 'ACTIVE', 'SYSTEM'),
(3, 'staff_daotao', '$2a$10$EblZqNptyYvcLm/VwDCVAuBjzZOI7khzdyGPBr08PpIi0na624b8.', 'ThS. Phạm Thị Thu Sương', 'daotao.staff@hcmute.edu.vn', 2, 3, 'ACTIVE', 'SYSTEM'),
(4, 'staff_academic', '$2a$10$EblZqNptyYvcLm/VwDCVAuBjzZOI7khzdyGPBr08PpIi0na624b8.', 'Cán bộ Quản lý Đào tạo', 'staff.academic@hcmute.edu.vn', 2, 3, 'ACTIVE', 'SYSTEM'),
(5, 'staff_tuyensinh', '$2a$10$EblZqNptyYvcLm/VwDCVAuBjzZOI7khzdyGPBr08PpIi0na624b8.', 'ThS. Nguyễn Văn Tuyển Sinh', 'tuyensinh.staff@hcmute.edu.vn', 2, 2, 'ACTIVE', 'SYSTEM'),
(6, 'staff_cntt', '$2a$10$EblZqNptyYvcLm/VwDCVAuBjzZOI7khzdyGPBr08PpIi0na624b8.', 'TS. Trần Khoa CNTT', 'cntt.staff@hcmute.edu.vn', 2, 4, 'ACTIVE', 'SYSTEM'),
(7, 'staff_doan', '$2a$10$EblZqNptyYvcLm/VwDCVAuBjzZOI7khzdyGPBr08PpIi0na624b8.', 'Đ/c Lê Bí Thư Đoàn', 'doantn.staff@hcmute.edu.vn', 2, 1, 'ACTIVE', 'SYSTEM'),
(8, 'staff_nn', '$2a$10$EblZqNptyYvcLm/VwDCVAuBjzZOI7khzdyGPBr08PpIi0na624b8.', 'ThS. Giảng Viên Ngoại Ngữ', 'nn.staff@hcmute.edu.vn', 2, 5, 'ACTIVE', 'SYSTEM'),
(9, 'student', '$2a$10$EblZqNptyYvcLm/VwDCVAuBjzZOI7khzdyGPBr08PpIi0na624b8.', 'Nguyễn Văn Sinh Viên Mẫu', 'student@student.hcmute.edu.vn', 1, 4, 'ACTIVE', 'SYSTEM'),
(10, 'student01', '$2a$10$EblZqNptyYvcLm/VwDCVAuBjzZOI7khzdyGPBr08PpIi0na624b8.', 'Nguyễn Văn Sinh Viên', 'student01@hcmute.edu.vn', 1, 4, 'ACTIVE', 'SYSTEM'),
(11, 'lecuong', '$2a$10$EblZqNptyYvcLm/VwDCVAuBjzZOI7khzdyGPBr08PpIi0na624b8.', 'Lê Quốc Cường', 'lecuong@gmail.com', 1, 4, 'ACTIVE', 'SYSTEM'),
(12, 'minhquan', '$2a$10$EblZqNptyYvcLm/VwDCVAuBjzZOI7khzdyGPBr08PpIi0na624b8.', 'Trần Minh Quân', 'minhquan@student.hcmute.edu.vn', 1, 4, 'ACTIVE', 'SYSTEM'),
(13, 'vanduc', '$2a$10$EblZqNptyYvcLm/VwDCVAuBjzZOI7khzdyGPBr08PpIi0na624b8.', 'Phạm Văn Đức', 'vanduc@student.hcmute.edu.vn', 1, 4, 'ACTIVE', 'SYSTEM'),
(14, 'camtu', '$2a$10$EblZqNptyYvcLm/VwDCVAuBjzZOI7khzdyGPBr08PpIi0na624b8.', 'Đặng Thị Cẩm Tú', 'camtu@student.hcmute.edu.vn', 1, 6, 'ACTIVE', 'SYSTEM')
ON DUPLICATE KEY UPDATE full_name=VALUES(full_name);
