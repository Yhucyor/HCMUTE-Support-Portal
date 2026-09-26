-- ==============================================================================
-- DATABASE SCHEMA DDL FOR QAUTE PORTAL (SPRING BOOT 3 + MYSQL 8 / POSTGRESQL)
-- Standard: 3NF Normalized, Composite Indexes, Check Constraints
-- ==============================================================================

SET FOREIGN_KEY_CHECKS = 0;
DROP TABLE IF EXISTS faqs;
DROP TABLE IF EXISTS ticket_histories;
DROP TABLE IF EXISTS attachments;
DROP TABLE IF EXISTS messages;
DROP TABLE IF EXISTS tickets;
DROP TABLE IF EXISTS conversations;
DROP TABLE IF EXISTS users;
DROP TABLE IF EXISTS departments;
DROP TABLE IF EXISTS roles;
SET FOREIGN_KEY_CHECKS = 1;

-- ------------------------------------------------------------------------------
-- 1. BẢNG ROLES (Vai trò: ROLE_STUDENT, ROLE_STAFF, ROLE_ADMIN)
-- ------------------------------------------------------------------------------
CREATE TABLE roles (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE,
    description VARCHAR(255) NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by VARCHAR(100) DEFAULT 'SYSTEM',
    is_deleted BOOLEAN DEFAULT FALSE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO roles (id, name, description) VALUES
(1, 'ROLE_STUDENT', 'Sinh viên chính quy / chất lượng cao'),
(2, 'ROLE_STAFF', 'Cán bộ tư vấn theo Khoa / Phòng ban'),
(3, 'ROLE_ADMIN', 'Quản trị viên hệ thống');

-- ------------------------------------------------------------------------------
-- 2. BẢNG DEPARTMENTS (Khoa, Phòng Tuyển sinh, Đoàn Thanh niên, Phòng Đào tạo)
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
    created_by VARCHAR(100) DEFAULT 'SYSTEM',
    is_deleted BOOLEAN DEFAULT FALSE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO departments (id, name, code, office_location, contact_email, contact_phone, description) VALUES
(1, 'Đoàn Thanh niên - Hội Sinh viên', 'DOAN_HOI', 'A1-102', 'doantn@hcmute.edu.vn', '028.3722.1223', 'Phong trào, tình nguyện, video truyền thông'),
(2, 'Phòng Tuyển sinh & Truyền thông', 'TUYEN_SINH', 'A1-101', 'tuyensinh@hcmute.edu.vn', '028.3722.5766', 'Tư vấn tuyển sinh, đề án tuyển sinh'),
(3, 'Phòng Đào tạo & Công tác Sinh viên', 'DAO_TAO', 'A1-201', 'daotao@hcmute.edu.vn', '028.3896.8641', 'Học vụ, điểm số, đăng ký môn học, chứng chỉ TOEIC'),
(4, 'Khoa Công nghệ Thông tin', 'KHOA_CNTT', 'E1-402', 'cntt@hcmute.edu.vn', '028.3897.2092', 'Đồ án tốt nghiệp, học phần chuyên ngành CNTT'),
(5, 'Khoa Ngoại ngữ', 'KHOA_NN', 'A1-306', 'nn@hcmute.edu.vn', '028.3896.1373', 'Chuyển điểm chuẩn đầu ra ngoại ngữ, thi ĐGNLTA');

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
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by VARCHAR(100) DEFAULT 'SYSTEM',
    is_deleted BOOLEAN DEFAULT FALSE,
    CONSTRAINT fk_users_role FOREIGN KEY (role_id) REFERENCES roles(id),
    CONSTRAINT fk_users_department FOREIGN KEY (department_id) REFERENCES departments(id),
    CONSTRAINT chk_user_status CHECK (status IN ('ACTIVE', 'PENDING_ACTIVATION', 'LOCKED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ------------------------------------------------------------------------------
-- 4. BẢNG CONVERSATIONS (Phiên tư vấn & Chatbot AI)
-- ------------------------------------------------------------------------------
CREATE TABLE conversations (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    creator_id BIGINT NULL, -- NULL nếu là Guest
    guest_email VARCHAR(150) NULL,
    department_id BIGINT NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE', -- ACTIVE, CONVERTED_TO_TICKET, CLOSED
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by VARCHAR(100) DEFAULT 'SYSTEM',
    is_deleted BOOLEAN DEFAULT FALSE,
    CONSTRAINT fk_conversations_creator FOREIGN KEY (creator_id) REFERENCES users(id),
    CONSTRAINT fk_conversations_department FOREIGN KEY (department_id) REFERENCES departments(id),
    CONSTRAINT chk_conversation_status CHECK (status IN ('ACTIVE', 'CONVERTED_TO_TICKET', 'CLOSED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ------------------------------------------------------------------------------
-- 5. BẢNG MESSAGES (Tin nhắn trong phiên hội thoại)
-- Composite Index: (conversation_id, created_at)
-- ------------------------------------------------------------------------------
CREATE TABLE messages (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    conversation_id BIGINT NOT NULL,
    sender_id BIGINT NULL, -- NULL nếu là Guest
    sender_type VARCHAR(20) NOT NULL, -- STUDENT, GUEST, STAFF, AI_BOT
    content TEXT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by VARCHAR(100) DEFAULT 'SYSTEM',
    is_deleted BOOLEAN DEFAULT FALSE,
    CONSTRAINT fk_messages_conversation FOREIGN KEY (conversation_id) REFERENCES conversations(id) ON DELETE CASCADE,
    CONSTRAINT fk_messages_sender_user FOREIGN KEY (sender_id) REFERENCES users(id),
    CONSTRAINT chk_sender_type CHECK (sender_type IN ('STUDENT', 'GUEST', 'STAFF', 'AI_BOT')),
    INDEX idx_messages_conversation_created (conversation_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ------------------------------------------------------------------------------
-- 6. BẢNG ATTACHMENTS (Tệp tin đính kèm: PDF, Hình ảnh, Word, Excel, Video)
-- ------------------------------------------------------------------------------
CREATE TABLE attachments (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    file_name VARCHAR(255) NOT NULL,
    file_url VARCHAR(500) NOT NULL,
    file_type VARCHAR(50) NOT NULL, -- PDF, IMAGE, DOCX, XLSX, MP4
    file_size BIGINT NOT NULL,
    message_id BIGINT NULL,
    ticket_id BIGINT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by VARCHAR(100) DEFAULT 'SYSTEM',
    is_deleted BOOLEAN DEFAULT FALSE,
    CONSTRAINT fk_attachments_message FOREIGN KEY (message_id) REFERENCES messages(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ------------------------------------------------------------------------------
-- 7. BẢNG TICKETS (Quản lý yêu cầu tư vấn học vụ & SLA)
-- Composite Index: (department_id, status, due_date)
-- Check Constraints: priority & status
-- ------------------------------------------------------------------------------
CREATE TABLE tickets (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    ticket_code VARCHAR(50) NOT NULL UNIQUE, -- TK-20260905-XXXX
    title VARCHAR(255) NOT NULL,
    description TEXT NOT NULL,
    conversation_id BIGINT NULL, -- Liên kết nếu chuyển đổi từ Chat
    department_id BIGINT NOT NULL,
    creator_id BIGINT NULL, -- Nullable cho Guest
    guest_name VARCHAR(150) NULL,
    guest_email VARCHAR(150) NULL,
    guest_token VARCHAR(100) NULL,
    assigned_to BIGINT NULL, -- Staff ID tiếp nhận
    priority VARCHAR(20) NOT NULL DEFAULT 'MEDIUM',
    status VARCHAR(30) NOT NULL DEFAULT 'OPEN',
    due_date TIMESTAMP NOT NULL,
    resolved_at TIMESTAMP NULL,
    closed_at TIMESTAMP NULL,
    rating TINYINT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by VARCHAR(100) DEFAULT 'SYSTEM',
    is_deleted BOOLEAN DEFAULT FALSE,
    CONSTRAINT fk_tickets_conversation FOREIGN KEY (conversation_id) REFERENCES conversations(id),
    CONSTRAINT fk_tickets_dept FOREIGN KEY (department_id) REFERENCES departments(id),
    CONSTRAINT fk_tickets_creator_user FOREIGN KEY (creator_id) REFERENCES users(id),
    CONSTRAINT fk_tickets_staff_assigned FOREIGN KEY (assigned_to) REFERENCES users(id),
    CONSTRAINT chk_ticket_priority CHECK (priority IN ('LOW', 'MEDIUM', 'HIGH', 'URGENT')),
    CONSTRAINT chk_ticket_status CHECK (status IN ('OPEN', 'IN_PROGRESS', 'WAITING_STUDENT', 'RESOLVED', 'CLOSED', 'OVERDUE')),
    INDEX idx_tickets_dept_status_due (department_id, status, due_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

ALTER TABLE attachments ADD CONSTRAINT fk_attachments_ticket FOREIGN KEY (ticket_id) REFERENCES tickets(id) ON DELETE CASCADE;

-- ------------------------------------------------------------------------------
-- 8. BẢNG TICKET_HISTORIES (Nhật ký vòng đời & chuyển đổi trạng thái Ticket)
-- ------------------------------------------------------------------------------
CREATE TABLE ticket_histories (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    ticket_id BIGINT NOT NULL,
    actor_id BIGINT NULL,
    actor_name VARCHAR(150) NOT NULL,
    from_status VARCHAR(30) NULL,
    to_status VARCHAR(30) NOT NULL,
    action_note TEXT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by VARCHAR(100) DEFAULT 'SYSTEM',
    is_deleted BOOLEAN DEFAULT FALSE,
    CONSTRAINT fk_histories_ticket FOREIGN KEY (ticket_id) REFERENCES tickets(id) ON DELETE CASCADE,
    CONSTRAINT fk_histories_actor FOREIGN KEY (actor_id) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ------------------------------------------------------------------------------
-- 9. BẢNG FAQS (Câu hỏi thường gặp & Tri thức AI Matcher)
-- ------------------------------------------------------------------------------
CREATE TABLE faqs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    question VARCHAR(500) NOT NULL,
    answer TEXT NOT NULL,
    department_id BIGINT NOT NULL,
    category VARCHAR(100) NULL,
    keywords VARCHAR(255) NULL,
    view_count INT DEFAULT 0,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by VARCHAR(100) DEFAULT 'SYSTEM',
    is_deleted BOOLEAN DEFAULT FALSE,
    CONSTRAINT fk_faqs_department FOREIGN KEY (department_id) REFERENCES departments(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
