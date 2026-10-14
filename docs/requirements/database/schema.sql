-- ==============================================================================
-- DATABASE SCHEMA DDL FOR QAUTE PORTAL (SPRING BOOT 3 + MYSQL 8)
-- Charset: utf8mb4 | Collation: utf8mb4_unicode_ci
-- Designed for 3NF, SLA Engine, RBAC, Moderation, and AI Knowledge Storage
-- ==============================================================================

SET FOREIGN_KEY_CHECKS = 0;
DROP TABLE IF EXISTS ai_knowledge_documents;
DROP TABLE IF EXISTS notification_logs;
DROP TABLE IF EXISTS post_reactions;
DROP TABLE IF EXISTS post_comments;
DROP TABLE IF EXISTS post_reports;
DROP TABLE IF EXISTS post_attachments;
DROP TABLE IF EXISTS posts;
DROP TABLE IF EXISTS ticket_messages;
DROP TABLE IF EXISTS tickets;
DROP TABLE IF EXISTS otp_tokens;
DROP TABLE IF EXISTS users;
DROP TABLE IF EXISTS departments;
DROP TABLE IF EXISTS roles;
SET FOREIGN_KEY_CHECKS = 1;

-- ------------------------------------------------------------------------------
-- 1. BẢNG ROLES (Vai trò hệ thống: GUEST, ROLE_STUDENT, ROLE_STAFF, ROLE_ADMIN)
-- ------------------------------------------------------------------------------
CREATE TABLE roles (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE,
    description VARCHAR(255) NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_deleted BOOLEAN DEFAULT FALSE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO roles (id, name, description) VALUES
(1, 'ROLE_STUDENT', 'Sinh viên đại học chính quy / chất lượng cao'),
(2, 'ROLE_STAFF', 'Cán bộ / Giảng viên thuộc Khoa / Phòng ban'),
(3, 'ROLE_ADMIN', 'Quản trị viên toàn quyền hệ thống');

-- ------------------------------------------------------------------------------
-- 2. BẢNG DEPARTMENTS (Khoa, Phòng Đào tạo, Tuyển sinh, Đoàn Thanh niên...)
-- ------------------------------------------------------------------------------
CREATE TABLE departments (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(150) NOT NULL UNIQUE,
    code VARCHAR(50) NOT NULL UNIQUE,
    office_location VARCHAR(100) NULL,
    contact_email VARCHAR(100) NULL,
    contact_phone VARCHAR(50) NULL,
    description TEXT NULL,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_deleted BOOLEAN DEFAULT FALSE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO departments (id, name, code, office_location, contact_email, contact_phone, description) VALUES
(1, 'Đoàn Thanh niên - Hội Sinh viên', 'DOAN_HOI', 'Phòng A1-102', 'doantn@hcmute.edu.vn', '028.3722.1223', 'Phong trào sinh viên, tình nguyện, rèn luyện, truyền thông'),
(2, 'Phòng Tuyển sinh & Truyền thông', 'TUYEN_SINH', 'Phòng A1-101', 'tuyensinh@hcmute.edu.vn', '028.3722.5766', 'Tư vấn tuyển sinh các hệ, truyền thông, đề án tuyển sinh'),
(3, 'Phòng Đào tạo & Công tác Sinh viên', 'DAO_TAO', 'Phòng A1-201', 'daotao@hcmute.edu.vn', '028.3896.8641', 'Quản lý học vụ, đăng ký môn học, điểm thi, chứng chỉ TOEIC, học bổng'),
(4, 'Khoa Công nghệ Thông tin', 'KHOA_CNTT', 'Tòa nhà E1-402', 'cntt@hcmute.edu.vn', '028.3897.2092', 'Đồ án tốt nghiệp, học phần chuyên ngành CNTT, thực tập doanh nghiệp'),
(5, 'Khoa Ngoại ngữ', 'KHOA_NN', 'Tòa nhà A1-306', 'nn@hcmute.edu.vn', '028.3896.1373', 'Chuyển điểm chuẩn đầu ra tiếng Anh, kỳ thi ĐGNLTA đầu vào');

-- ------------------------------------------------------------------------------
-- 3. BẢNG USERS (Người dùng hệ thống)
-- ------------------------------------------------------------------------------
CREATE TABLE users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(100) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    full_name VARCHAR(150) NOT NULL,
    email VARCHAR(150) NOT NULL UNIQUE,
    phone VARCHAR(30) NULL,
    avatar_url VARCHAR(500) NULL,
    role_id BIGINT NOT NULL,
    department_id BIGINT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE', -- ACTIVE, PENDING_ACTIVATION, LOCKED
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_deleted BOOLEAN DEFAULT FALSE,
    CONSTRAINT fk_users_role FOREIGN KEY (role_id) REFERENCES roles(id),
    CONSTRAINT fk_users_department FOREIGN KEY (department_id) REFERENCES departments(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ------------------------------------------------------------------------------
-- 4. BẢNG OTP_TOKENS (Xác thực tài khoản qua Email OTP)
-- ------------------------------------------------------------------------------
CREATE TABLE otp_tokens (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    email VARCHAR(150) NOT NULL,
    otp_code VARCHAR(10) NOT NULL,
    purpose VARCHAR(50) NOT NULL, -- REGISTRATION, PASSWORD_RESET
    expired_at TIMESTAMP NOT NULL,
    is_used BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ------------------------------------------------------------------------------
-- 5. BẢNG TICKETS (Quản lý yêu cầu tư vấn học vụ & SLA)
-- ------------------------------------------------------------------------------
CREATE TABLE tickets (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    ticket_code VARCHAR(50) NOT NULL UNIQUE, -- TK-20260905-XXXX
    title VARCHAR(255) NOT NULL,
    description TEXT NOT NULL,
    source_type VARCHAR(30) NOT NULL DEFAULT 'DIRECT_FORM', -- DIRECT_FORM, CHAT_CONVERSION
    department_id BIGINT NOT NULL,
    creator_id BIGINT NULL, -- Nullable khi Guest gửi Ticket
    guest_name VARCHAR(150) NULL,
    guest_email VARCHAR(150) NULL,
    guest_phone VARCHAR(30) NULL,
    guest_access_token VARCHAR(100) NULL, -- Token bí mật cho Guest tra cứu
    assigned_to BIGINT NULL, -- Staff ID tiếp nhận xử lý
    priority VARCHAR(20) NOT NULL DEFAULT 'MEDIUM', -- URGENT, MEDIUM, LOW
    status VARCHAR(30) NOT NULL DEFAULT 'OPEN', -- OPEN, IN_PROGRESS, WAITING_STUDENT, RESOLVED, CLOSED, OVERDUE
    due_date TIMESTAMP NOT NULL, -- Tính tự động theo SLA Engine
    resolved_at TIMESTAMP NULL,
    closed_at TIMESTAMP NULL,
    rating TINYINT NULL, -- 1-5 sao đánh giá chất lượng tư vấn
    rating_feedback TEXT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_deleted BOOLEAN DEFAULT FALSE,
    CONSTRAINT fk_tickets_department FOREIGN KEY (department_id) REFERENCES departments(id),
    CONSTRAINT fk_tickets_creator FOREIGN KEY (creator_id) REFERENCES users(id),
    CONSTRAINT fk_tickets_assigned_staff FOREIGN KEY (assigned_to) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ------------------------------------------------------------------------------
-- 6. BẢNG TICKET_MESSAGES (Lịch sử trao đổi trong Ticket)
-- ------------------------------------------------------------------------------
CREATE TABLE ticket_messages (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    ticket_id BIGINT NOT NULL,
    sender_id BIGINT NULL, -- NULL nếu là Guest gửi
    sender_type VARCHAR(20) NOT NULL, -- STUDENT, GUEST, STAFF, SYSTEM
    content TEXT NOT NULL,
    attachment_url VARCHAR(500) NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    is_deleted BOOLEAN DEFAULT FALSE,
    CONSTRAINT fk_messages_ticket FOREIGN KEY (ticket_id) REFERENCES tickets(id) ON DELETE CASCADE,
    CONSTRAINT fk_messages_sender FOREIGN KEY (sender_id) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ------------------------------------------------------------------------------
-- 7. BẢNG POSTS (Bảng tin chính thức & Diễn đàn sinh viên)
-- ------------------------------------------------------------------------------
CREATE TABLE posts (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    content LONGTEXT NOT NULL,
    post_type VARCHAR(30) NOT NULL DEFAULT 'STUDENT_FORUM', -- OFFICIAL_ANNOUNCEMENT, STUDENT_FORUM
    department_id BIGINT NULL, -- Đơn vị ban hành (với bài chính thức)
    author_id BIGINT NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING_APPROVAL', -- PENDING_APPROVAL, APPROVED, REJECTED, HIDDEN
    rejection_reason VARCHAR(255) NULL,
    approved_by BIGINT NULL,
    approved_at TIMESTAMP NULL,
    is_pinned BOOLEAN DEFAULT FALSE,
    view_count INT DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_deleted BOOLEAN DEFAULT FALSE,
    CONSTRAINT fk_posts_author FOREIGN KEY (author_id) REFERENCES users(id),
    CONSTRAINT fk_posts_approver FOREIGN KEY (approved_by) REFERENCES users(id),
    CONSTRAINT fk_posts_department FOREIGN KEY (department_id) REFERENCES departments(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ------------------------------------------------------------------------------
-- 8. BẢNG POST_ATTACHMENTS (Tệp đính kèm: PDF, Word, Excel, Video MP4)
-- ------------------------------------------------------------------------------
CREATE TABLE post_attachments (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    post_id BIGINT NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    file_url VARCHAR(500) NOT NULL,
    file_type VARCHAR(50) NOT NULL, -- PDF, DOCX, XLSX, MP4, IMAGE
    file_size BIGINT NOT NULL,
    source_type VARCHAR(30) DEFAULT 'DIRECT_UPLOAD', -- DIRECT_UPLOAD, NODEJS_WEBHOOK
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    is_deleted BOOLEAN DEFAULT FALSE,
    CONSTRAINT fk_attachments_post FOREIGN KEY (post_id) REFERENCES posts(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ------------------------------------------------------------------------------
-- 9. BẢNG POST_REPORTS (Báo cáo vi phạm bài viết)
-- ------------------------------------------------------------------------------
CREATE TABLE post_reports (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    post_id BIGINT NOT NULL,
    reporter_id BIGINT NOT NULL,
    reason VARCHAR(50) NOT NULL, -- SPAM, INAPPROPRIATE_LANGUAGE, WRONG_ACADEMIC_INFO, OTHER
    details TEXT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING', -- PENDING, RESOLVED, DISMISSED
    reviewed_by BIGINT NULL,
    reviewed_at TIMESTAMP NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_reports_post FOREIGN KEY (post_id) REFERENCES posts(id) ON DELETE CASCADE,
    CONSTRAINT fk_reports_reporter FOREIGN KEY (reporter_id) REFERENCES users(id),
    CONSTRAINT fk_reports_reviewer FOREIGN KEY (reviewed_by) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ------------------------------------------------------------------------------
-- 10. BẢNG POST_COMMENTS (Bình luận bài viết)
-- ------------------------------------------------------------------------------
CREATE TABLE post_comments (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    post_id BIGINT NOT NULL,
    author_id BIGINT NOT NULL,
    parent_id BIGINT NULL, -- Phục vụ comment lồng nhau (nested comments)
    content TEXT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_deleted BOOLEAN DEFAULT FALSE,
    CONSTRAINT fk_comments_post FOREIGN KEY (post_id) REFERENCES posts(id) ON DELETE CASCADE,
    CONSTRAINT fk_comments_author FOREIGN KEY (author_id) REFERENCES users(id),
    CONSTRAINT fk_comments_parent FOREIGN KEY (parent_id) REFERENCES post_comments(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ------------------------------------------------------------------------------
-- 11. BẢNG POST_REACTIONS (Thả cảm xúc / Like)
-- ------------------------------------------------------------------------------
CREATE TABLE post_reactions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    post_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    reaction_type VARCHAR(20) NOT NULL DEFAULT 'LIKE', -- LIKE, HEART
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_post_user_reaction (post_id, user_id),
    CONSTRAINT fk_reactions_post FOREIGN KEY (post_id) REFERENCES posts(id) ON DELETE CASCADE,
    CONSTRAINT fk_reactions_user FOREIGN KEY (user_id) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ------------------------------------------------------------------------------
-- 12. BẢNG AI_KNOWLEDGE_DOCUMENTS (Tài liệu tri thức RAG)
-- ------------------------------------------------------------------------------
CREATE TABLE ai_knowledge_documents (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    source_type VARCHAR(50) NOT NULL, -- PDF_REGULATION, ADMISSION_BROCHURE, FAQ_DATASET
    department_id BIGINT NULL,
    document_url VARCHAR(500) NULL,
    total_chunks INT DEFAULT 0,
    indexed_status VARCHAR(30) NOT NULL DEFAULT 'READY', -- PENDING, INDEXING, READY, FAILED
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_deleted BOOLEAN DEFAULT FALSE,
    CONSTRAINT fk_ai_docs_department FOREIGN KEY (department_id) REFERENCES departments(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ------------------------------------------------------------------------------
-- 13. BẢNG NOTIFICATION_LOGS (Lịch sử gửi Email & Thông báo hệ thống)
-- ------------------------------------------------------------------------------
CREATE TABLE notification_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    recipient_email VARCHAR(150) NOT NULL,
    subject VARCHAR(255) NOT NULL,
    template_type VARCHAR(50) NOT NULL, -- TICKET_CREATED, TICKET_RESOLVED, POST_APPROVED, POST_REPORTED
    status VARCHAR(30) NOT NULL DEFAULT 'SUCCESS', -- SUCCESS, FAILED
    error_message TEXT NULL,
    sent_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
