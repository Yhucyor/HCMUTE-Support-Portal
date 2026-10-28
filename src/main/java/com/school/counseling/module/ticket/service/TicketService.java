package com.school.counseling.module.ticket.service;

import com.school.counseling.common.exception.AccessDeniedBusinessException;
import com.school.counseling.common.exception.ResourceNotFoundException;
import com.school.counseling.common.exception.TicketAlreadyClaimedException;
import com.school.counseling.common.storage.IStorageService;
import com.school.counseling.module.auth.entity.Department;
import com.school.counseling.module.auth.entity.User;
import com.school.counseling.module.auth.repository.DepartmentRepository;
import com.school.counseling.module.auth.repository.UserRepository;
import com.school.counseling.module.ticket.entity.Attachment;
import com.school.counseling.module.ticket.repository.AttachmentRepository;
import com.school.counseling.module.ticket.dto.*;
import com.school.counseling.module.ticket.dto.TicketResponseDto.AttachmentDto;
import com.school.counseling.module.ticket.dto.TicketResponseDto.HistoryDto;
import com.school.counseling.module.ticket.entity.Ticket;
import com.school.counseling.module.ticket.entity.TicketHistory;
import com.school.counseling.module.ticket.entity.TicketTransferHistory;
import com.school.counseling.module.auth.dto.UserPrincipal;
import com.school.counseling.module.ticket.event.TicketClaimedEvent;
import com.school.counseling.module.ticket.event.TicketCreatedEvent;
import com.school.counseling.module.ticket.event.TicketResolvedEvent;
import com.school.counseling.module.ticket.event.TicketTransferredEvent;
import com.school.counseling.module.ticket.repository.TicketHistoryRepository;
import com.school.counseling.module.ticket.repository.TicketRepository;
import com.school.counseling.module.ticket.repository.TicketTransferHistoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import com.school.counseling.module.admin.dto.DepartmentAdminDto;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TicketService {

    private final TicketRepository ticketRepository;
    private final TicketHistoryRepository ticketHistoryRepository;
    private final TicketTransferHistoryRepository ticketTransferHistoryRepository;
    private final DepartmentRepository departmentRepository;
    private final UserRepository userRepository;
    private final AttachmentRepository attachmentRepository;
    private final SlaCalculatorService slaCalculatorService;
    private final IStorageService storageService;
    private final ApplicationEventPublisher eventPublisher;

    @Value("${app.server.base-url:http://localhost:8080}")
    private String serverBaseUrl = "http://localhost:8080";

    // ─── NHÓM TẠO TICKET ───────────────────────────────────────────────────

    @Transactional
    @CacheEvict(value = "user_ticket_counts", allEntries = true)
    public TicketResponseDto createTicket(CreateTicketRequest request, Long creatorId, List<MultipartFile> files) {
        Department department = departmentRepository.findById(request.getDepartmentId())
                .orElseThrow(() -> new ResourceNotFoundException("Đơn vị", "id", request.getDepartmentId()));

        if (department.getIsActive() != null && !department.getIsActive()) {
            throw new IllegalArgumentException("Khoa / Đơn vị [" + department.getName() + "] hiện đang tạm dừng tiếp nhận yêu cầu hỗ trợ mới.");
        }

        User creator = null;
        String guestToken = null;
        if (creatorId != null) {
            creator = userRepository.findById(creatorId).orElse(null);
        } else {
            String email = request.getGuestEmail() != null ? request.getGuestEmail().trim() : null;
            if (email == null || email.isBlank()) {
                throw new IllegalArgumentException("Vui lòng nhập địa chỉ email hợp lệ để nhận mã tra cứu và thông báo xử lý yêu cầu.");
            }
            request.setGuestEmail(email);

            String name = request.getGuestName() != null ? request.getGuestName().trim() : null;
            if (name == null || name.isBlank()) {
                request.setGuestName("Khách vãng lai");
            } else {
                request.setGuestName(name);
            }
            guestToken = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        }

        // Tự động suy diễn Mức độ ưu tiên và Hạn chót SLA từ Loại thủ tục học vụ
        String priority = slaCalculatorService.resolvePriority(request.getTopicCategory());
        LocalDateTime dueDate = slaCalculatorService.calculateDueDateByCategory(request.getTopicCategory());

        // AI Triage hỗ trợ nâng hạng ưu tiên nếu phát hiện tình huống khẩn cấp bất thường
        if (!"URGENT".equalsIgnoreCase(priority)) {
            try {
                TicketTriageResponseDto triage = suggestTicketTriage(request.getTitle(), request.getDescription());
                if (triage != null && ("URGENT".equalsIgnoreCase(triage.getPriority()) || "HIGH".equalsIgnoreCase(triage.getPriority()))) {
                    priority = triage.getPriority();
                    dueDate = slaCalculatorService.calculateDueDate(priority);
                    log.info("AI Triage phát hiện mức độ cấp bách, tự động nâng cấp độ ưu tiên Ticket lên: {}", priority);
                }
            } catch (Exception e) {
                log.debug("Không thể chạy AI Triage: {}", e.getMessage());
            }
        }

        String ticketCode = generateTicketCode();

        Ticket ticket = Ticket.builder()
                .ticketCode(ticketCode)
                .title(request.getTitle())
                .description(request.getDescription())
                .department(department)
                .creator(creator)
                .guestName(request.getGuestName())
                .guestEmail(request.getGuestEmail())
                .guestToken(guestToken)
                .priority(priority)
                .topicCategory(request.getTopicCategory())
                .status("OPEN")
                .dueDate(dueDate)
                .build();

        ticket = ticketRepository.save(ticket);

        TicketHistory initialHistory = saveHistory(ticket, creator,
                creator != null ? creator.getFullName() : (request.getGuestName() != null ? request.getGuestName() : "Khách vãng lai"),
                null, "OPEN", "Khởi tạo yêu cầu tư vấn mới: " + ticket.getDescription());

        saveAttachments(ticket, initialHistory, files, "ticket_attachments");

        log.info("Tạo mới Ticket thành công: Code={}, DueDate={}", ticket.getTicketCode(), ticket.getDueDate());

        String recipientEmail = ticket.getGuestEmail() != null ? ticket.getGuestEmail()
                : (ticket.getCreator() != null ? ticket.getCreator().getEmail() : null);
        String recipientName = ticket.getGuestName() != null ? ticket.getGuestName()
                : (ticket.getCreator() != null ? ticket.getCreator().getFullName() : "Bạn");
        String trackingUrl = buildTrackingUrl(ticket);

        eventPublisher.publishEvent(new TicketCreatedEvent(
                ticket.getId(),
                ticket.getTicketCode(),
                ticket.getTitle(),
                ticket.getPriority() != null ? ticket.getPriority() : "MEDIUM",
                ticket.getDueDate(),
                recipientEmail,
                recipientName,
                trackingUrl
        ));

        return mapToDetailDto(ticket);
    }

    public TicketTriageResponseDto suggestTicketTriage(String title, String description) {
        String safeTitle = title != null ? title.trim() : "";
        String safeDesc = description != null ? description.trim() : "";
        String text = (safeTitle + " " + safeDesc).toLowerCase();

        String deptCode = "DAO_TAO";
        String priority = "MEDIUM";
        String sentiment = "NEUTRAL";
        String action = "Tiếp nhận và phản hồi theo quy định tiếp nhận giải quyết thắc mắc học vụ.";

        if (text.contains("tuyển sinh") || text.contains("nhập học") || text.contains("trúng tuyển")) {
            deptCode = "TUYEN_SINH";
            action = "Cung cấp thông tin đề án tuyển sinh và hướng dẫn thủ tục nhập học.";
        } else if (text.contains("cntt") || text.contains("đồ án") || text.contains("phần mềm")) {
            deptCode = "KHOA_CNTT";
            action = "Chuyển tiếp đến Trợ lý Giáo vụ Khoa CNTT hoặc Giảng viên hướng dẫn.";
        } else if (text.contains("ngoại ngữ") || text.contains("toeic") || text.contains("đgnlta")) {
            deptCode = "KHOA_NN";
            action = "Hướng dẫn thủ tục nộp chứng chỉ ngoại ngữ hoặc xét miễn học phần.";
        } else if (text.contains("đoàn") || text.contains("hội sinh viên") || text.contains("mùa hè xanh")) {
            deptCode = "DOAN_HOI";
            action = "Kiểm tra danh sách tham gia phong trào hoặc cấp giấy chứng nhận tình nguyện.";
        }

        if (text.contains("buộc thôi học") || text.contains("đình chỉ") || text.contains("khẩn cấp") || text.contains("gấp")) {
            priority = "URGENT";
            sentiment = "ANXIOUS";
        } else if (text.contains("phúc khảo") || text.contains("học bổng") || text.contains("khiếu nại")) {
            priority = "HIGH";
        }

        Department dept = departmentRepository.findByCode(deptCode)
                .orElseGet(() -> departmentRepository.findAll().stream().findFirst().orElse(null));

        return TicketTriageResponseDto.builder()
                .suggestedDepartmentId(dept != null ? dept.getId() : null)
                .departmentCode(dept != null ? dept.getCode() : deptCode)
                .departmentName(dept != null ? dept.getName() : "Phòng Đào tạo & Công tác Sinh viên")
                .priority(priority)
                .sentiment(sentiment)
                .confidenceScore(0.75)
                .summary("[" + priority + "] " + safeTitle)
                .suggestedAction(action)
                .keyIndicators(List.of("fallback_java_rule"))
                .classifiedBy("JAVA_RULE_ENGINE")
                .build();
    }

    // ─── NHÓM THAO TÁC TRẠNG THÁI ──────────────────────────────────────────

    @Transactional
    @CacheEvict(value = "user_ticket_counts", allEntries = true)
    public TicketResponseDto claimTicket(Long ticketId, Long staffId) {
        User staff = userRepository.findById(staffId)
                .orElseThrow(() -> new ResourceNotFoundException("Cán bộ", "id", staffId));

        validateTicketAccess(ticketId, staff);

        int updatedRows = ticketRepository.claimTicketAtomic(ticketId, staff);
        if (updatedRows == 0) {
            throw new TicketAlreadyClaimedException(
                    "Yêu cầu hỗ trợ này đã được Cán bộ khác tiếp nhận xử lý rồi. Vui lòng làm mới trang Dashboard.");
        }

        Ticket ticket = ticketRepository.findByIdWithRelations(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket", "id", ticketId));

        saveHistory(ticket, staff, staff.getFullName(), "OPEN", "IN_PROGRESS", "Cán bộ tiếp nhận xử lý yêu cầu");
        log.info("Cán bộ ID={} đã tiếp nhận thành công Ticket Code={}", staffId, ticket.getTicketCode());

        String recipientEmail = ticket.getGuestEmail() != null ? ticket.getGuestEmail()
                : (ticket.getCreator() != null ? ticket.getCreator().getEmail() : null);
        String recipientName = ticket.getGuestName() != null ? ticket.getGuestName()
                : (ticket.getCreator() != null ? ticket.getCreator().getFullName() : "Bạn");
        String trackingUrl = buildTrackingUrl(ticket);
        String deptName = ticket.getDepartment() != null ? ticket.getDepartment().getName() : "Khoa/Phòng";

        eventPublisher.publishEvent(new TicketClaimedEvent(
                ticket.getId(),
                ticket.getTicketCode(),
                recipientEmail,
                recipientName,
                staff.getFullName(),
                deptName,
                trackingUrl
        ));

        return mapToDetailDto(ticket);
    }

    /**
     * Quản trị viên (Admin) can thiệp khẩn cấp tiếp quản vé bị quá hạn SLA từ các đơn vị.
     */
    @Transactional
    @CacheEvict(value = "user_ticket_counts", allEntries = true)
    public TicketResponseDto adminTakeoverTicket(Long ticketId, Long adminId) {
        User admin = userRepository.findById(adminId)
                .orElseThrow(() -> new ResourceNotFoundException("Admin", "id", adminId));

        Ticket ticket = ticketRepository.findByIdWithRelations(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket", "id", ticketId));

        String oldStatus = ticket.getStatus();
        String oldAssignee = ticket.getAssignedTo() != null ? ticket.getAssignedTo().getFullName() : "Chưa có cán bộ";

        ticket.setAssignedTo(admin);
        ticket.setStatus("IN_PROGRESS");
        if (ticket.getDueDate() == null || ticket.getDueDate().isBefore(LocalDateTime.now())) {
            ticket.setDueDate(LocalDateTime.now().plusHours(24));
        }

        ticketRepository.save(ticket);

        saveHistory(ticket, admin, admin.getFullName(), oldStatus, "IN_PROGRESS",
                String.format("Quản trị viên (Admin %s) can thiệp tiếp quản vé từ [%s] do yêu cầu quá hạn SLA.", admin.getFullName(), oldAssignee));

        log.info("Admin ID={} đã can thiệp tiếp quản Ticket Code={} thành công", adminId, ticket.getTicketCode());
        return mapToDetailDto(ticket);
    }

    /**
     * Chuyển giao Ticket sang Khoa / Phòng ban tiếp nhận mới.
     * Cán bộ của đơn vị hiện tại hoặc Quản trị viên (Admin) mới có quyền chuyển giao.
     */
    @Transactional
    @CacheEvict(value = "user_ticket_counts", allEntries = true)
    public TicketResponseDto transferTicket(Long ticketId, TicketTransferRequest request, UserPrincipal currentUser) {
        if (currentUser == null) {
            throw new AccessDeniedBusinessException("Vui lòng đăng nhập để thực hiện chuyển giao ticket!");
        }

        Ticket ticket = ticketRepository.findByIdWithRelations(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket", "id", ticketId));

        // 1. Kiểm tra trạng thái vé: Không được chuyển nếu đã đóng hoặc đã hủy
        if ("CLOSED".equalsIgnoreCase(ticket.getStatus()) || "CANCELLED".equalsIgnoreCase(ticket.getStatus())) {
            throw new IllegalArgumentException("Không thể chuyển giao ticket đã đóng hoặc đã hủy.");
        }

        // 2. Thẩm định quyền: Phải là Admin hoặc thuộc Khoa/Phòng hiện tại đang giữ vé
        boolean isPrivileged = (currentUser.getRole() != null &&
                (currentUser.getRole().equalsIgnoreCase("ROLE_ADMIN")
                || currentUser.getRole().equalsIgnoreCase("ROLE_SUPER_ADMIN")
                || currentUser.getRole().equalsIgnoreCase("ROLE_SUPERADMIN")))
                || (currentUser.getAuthorities() != null && currentUser.getAuthorities().stream()
                        .anyMatch(a -> a.getAuthority().equalsIgnoreCase("ROLE_ADMIN")
                                || a.getAuthority().equalsIgnoreCase("ROLE_SUPER_ADMIN")));

        Long currentDeptId = ticket.getDepartment() != null ? ticket.getDepartment().getId() : null;
        if (!isPrivileged && !Objects.equals(currentDeptId, currentUser.getDepartmentId())) {
            throw new AccessDeniedBusinessException("Bạn chỉ có quyền chuyển giao yêu cầu thuộc đơn vị của mình!");
        }

        // 3. Đơn vị tiếp nhận mới phải khác đơn vị hiện tại
        if (Objects.equals(currentDeptId, request.getToDepartmentId())) {
            throw new IllegalArgumentException("Đơn vị tiếp nhận mới phải khác với đơn vị hiện tại của ticket!");
        }

        Department toDepartment = departmentRepository.findById(request.getToDepartmentId())
                .orElseThrow(() -> new ResourceNotFoundException("Đơn vị tiếp nhận mới", "id", request.getToDepartmentId()));

        if (toDepartment.getIsActive() != null && !toDepartment.getIsActive()) {
            throw new IllegalArgumentException("Đơn vị [" + toDepartment.getName() + "] hiện đang tạm dừng tiếp nhận yêu cầu.");
        }

        User transferrer = userRepository.findById(currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Cán bộ thực hiện", "id", currentUser.getId()));

        Department fromDepartment = ticket.getDepartment();

        // 4. Lưu bản ghi TicketTransferHistory
        TicketTransferHistory transferHistory = TicketTransferHistory.builder()
                .ticket(ticket)
                .fromDepartment(fromDepartment)
                .toDepartment(toDepartment)
                .transferredBy(transferrer)
                .reason(request.getReason().trim())
                .build();
        ticketTransferHistoryRepository.save(transferHistory);

        // 5. Cập nhật Ticket: Đổi đơn vị, reset cán bộ phụ trách, đưa trạng thái về OPEN để đơn vị mới tiếp nhận
        String oldStatus = ticket.getStatus();
        ticket.setDepartment(toDepartment);
        ticket.setAssignedTo(null);
        ticket.setStatus("OPEN");
        ticketRepository.save(ticket);

        // 6. Ghi nhận nhật ký TicketHistory
        String fromDeptName = fromDepartment != null ? fromDepartment.getName() : "Không xác định";
        String historyNote = String.format("Chuyển giao đơn vị: [%s] ➔ [%s]. Lý do: %s",
                fromDeptName, toDepartment.getName(), request.getReason().trim());
        saveHistory(ticket, transferrer, transferrer.getFullName(), oldStatus, "OPEN", historyNote);

        // 7. Bắn sự kiện TicketTransferredEvent
        String trackingUrl = buildTrackingUrl(ticket);
        eventPublisher.publishEvent(new TicketTransferredEvent(
                ticket.getId(),
                ticket.getTicketCode(),
                ticket.getTitle(),
                fromDeptName,
                toDepartment.getName(),
                transferrer.getFullName(),
                request.getReason().trim(),
                trackingUrl
        ));

        log.info("Ticket #{} đã được chuyển giao thành công từ [{}] sang [{}] bởi cán bộ [{}] (ID={})",
                ticket.getTicketCode(), fromDeptName, toDepartment.getName(), transferrer.getFullName(), transferrer.getId());

        return mapToDetailDto(ticket);
    }

    @Transactional
    public TicketResponseDto replyTicket(Long ticketId, TicketReplyRequest request, Long responderId, List<MultipartFile> files) {
        Ticket ticket = ticketRepository.findByIdWithRelations(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket", "id", ticketId));

        User responder = responderId != null ? userRepository.findById(responderId).orElse(null) : null;
        String responderName = responder != null ? responder.getFullName() : "Cán bộ tư vấn";

        boolean isStaff = responder != null && responder.getRole() != null &&
                ("ROLE_STAFF".equalsIgnoreCase(responder.getRole().getName()) || "ROLE_ADMIN".equalsIgnoreCase(responder.getRole().getName()));

        String fromStatus = ticket.getStatus();
        String toStatus = fromStatus;

        if (isStaff && request != null && request.getNewStatus() != null && !request.getNewStatus().isBlank()) {
            if ("RESOLVED".equalsIgnoreCase(request.getNewStatus())) {
                toStatus = "RESOLVED";
                ticket.setStatus("RESOLVED");
                ticket.setResolvedAt(LocalDateTime.now());
            } else if ("WAITING_STUDENT".equalsIgnoreCase(request.getNewStatus())) {
                toStatus = "WAITING_STUDENT";
                ticket.setStatus("WAITING_STUDENT");
            }
        }

        if (!isStaff) {
            ticket.setHasStudentReply(true);
            if ("WAITING_STUDENT".equalsIgnoreCase(ticket.getStatus())) {
                toStatus = "IN_PROGRESS";
                ticket.setStatus("IN_PROGRESS");
            }
        } else {
            ticket.setHasStudentReply(false);
        }

        ticket = ticketRepository.save(ticket);

        TicketHistory replyHistory = saveHistory(ticket, responder, responderName, fromStatus, toStatus,
                request != null ? request.getContent() : "");

        saveAttachments(ticket, replyHistory, files, "ticket_replies");

        log.info("Phản hồi Ticket Code={}: Status={}", ticket.getTicketCode(), toStatus);
        return mapToDetailDto(ticket);
    }

    @Transactional
    @CacheEvict(value = "user_ticket_counts", allEntries = true)
    public TicketResponseDto resolveTicket(Long ticketId, Long staffId, String resolutionNote) {
        Ticket ticket = ticketRepository.findByIdWithRelations(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket", "id", ticketId));

        User staff = staffId != null ? userRepository.findById(staffId).orElse(null) : null;
        if (staff != null) {
            validateTicketAccess(ticketId, staff);
        }
        String fromStatus = ticket.getStatus();

        ticket.setStatus("RESOLVED");
        ticket.setResolvedAt(LocalDateTime.now());
        ticket = ticketRepository.save(ticket);

        saveHistory(ticket, staff, staff != null ? staff.getFullName() : "Cán bộ",
                fromStatus, "RESOLVED", resolutionNote != null ? resolutionNote : "Đã hoàn tất giải đáp yêu cầu");

        String recipientEmail = ticket.getGuestEmail() != null ? ticket.getGuestEmail()
                : (ticket.getCreator() != null ? ticket.getCreator().getEmail() : null);
        String recipientName = ticket.getGuestName() != null ? ticket.getGuestName()
                : (ticket.getCreator() != null ? ticket.getCreator().getFullName() : "Bạn");
        String trackingUrl = buildTrackingUrl(ticket);

        eventPublisher.publishEvent(new TicketResolvedEvent(
                ticket.getId(),
                ticket.getTicketCode(),
                recipientEmail,
                recipientName,
                resolutionNote != null ? resolutionNote : "Yêu cầu đã được xử lý xong",
                trackingUrl
        ));

        return mapToDetailDto(ticket);
    }

    @Transactional
    @CacheEvict(value = "user_ticket_counts", allEntries = true)
    public TicketResponseDto closeTicket(Long ticketId, Long userId, Integer rating, String feedback) {
        Ticket ticket = ticketRepository.findByIdWithRelations(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket", "id", ticketId));

        User user = userId != null ? userRepository.findById(userId).orElse(null) : null;
        String fromStatus = ticket.getStatus();

        ticket.setStatus("CLOSED");
        ticket.setClosedAt(LocalDateTime.now());
        if (rating != null && rating >= 1 && rating <= 5) {
            ticket.setRating(rating);
        }
        ticket = ticketRepository.save(ticket);

        String note = "Đóng yêu cầu (Đánh giá CSAT: " + (rating != null ? rating + " sao" : "Không") + ")";
        if (feedback != null && !feedback.trim().isEmpty()) {
            note += " | Góp ý: " + feedback.trim();
        }

        saveHistory(ticket, user, user != null ? user.getFullName() : "Sinh viên",
                fromStatus, "CLOSED", note);

        return mapToDetailDto(ticket);
    }

    @Transactional
    public TicketResponseDto rateTicket(Long ticketId, Long userId, Integer rating, String feedback) {
        Ticket ticket = ticketRepository.findByIdWithRelations(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket", "id", ticketId));

        User user = userId != null ? userRepository.findById(userId).orElse(null) : null;
        if (rating != null && rating >= 1 && rating <= 5) {
            ticket.setRating(rating);
        }

        if ("RESOLVED".equalsIgnoreCase(ticket.getStatus())) {
            ticket.setStatus("CLOSED");
            ticket.setClosedAt(LocalDateTime.now());
        }

        ticket = ticketRepository.save(ticket);

        String note = "Sinh viên gửi đánh giá CSAT: " + (rating != null ? rating + " sao" : "Không");
        if (feedback != null && !feedback.trim().isEmpty()) {
            note += " | Góp ý: " + feedback.trim();
        }

        saveHistory(ticket, user, user != null ? user.getFullName() : "Sinh viên",
                ticket.getStatus(), ticket.getStatus(), note);

        return mapToDetailDto(ticket);
    }

    @Transactional
    public TicketResponseDto rateTicketByGuest(String token, Integer rating, String feedback) {
        Ticket ticket = ticketRepository.findByGuestTokenWithRelations(token)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket", "token", token));

        if (rating != null && rating >= 1 && rating <= 5) {
            ticket.setRating(rating);
        }

        if ("RESOLVED".equalsIgnoreCase(ticket.getStatus())) {
            ticket.setStatus("CLOSED");
            ticket.setClosedAt(LocalDateTime.now());
        }

        ticket = ticketRepository.save(ticket);

        String note = "Khách tra cứu gửi đánh giá CSAT: " + (rating != null ? rating + " sao" : "Không");
        if (feedback != null && !feedback.trim().isEmpty()) {
            note += " | Góp ý: " + feedback.trim();
        }

        saveHistory(ticket, null, ticket.getGuestName() != null ? ticket.getGuestName() : "Khách vãng lai",
                ticket.getStatus(), ticket.getStatus(), note);

        return mapToDetailDto(ticket);
    }

    // ─── NHÓM TRUY VẤN / ĐỌC DỮ LIỆU ────────────────────────────────────────

    public TicketResponseDto getTicketById(Long ticketId) {
        Ticket ticket = ticketRepository.findByIdWithRelations(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket", "id", ticketId));
        return mapToDetailDto(ticket);
    }

    public TicketResponseDto getTicketByCode(String ticketCode) {
        Ticket ticket = ticketRepository.findByTicketCodeWithRelations(ticketCode)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket", "code", ticketCode));
        return mapToDetailDto(ticket);
    }

    public TicketResponseDto getTicketByGuestToken(String token) {
        Ticket ticket = ticketRepository.findByGuestTokenWithRelations(token)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket", "token", token));
        return mapToDetailDto(ticket);
    }

    public List<TicketSummaryDto> getTicketSummaryByDepartment(Long departmentId, String status) {
        if ("OVERDUE".equalsIgnoreCase(status)) {
            return ticketRepository.findSummaryByDepartmentId(departmentId).stream()
                    .filter(TicketSummaryDto::isOverdue)
                    .toList();
        }
        if (status != null && !status.trim().isEmpty() && !"ALL".equalsIgnoreCase(status)) {
            return ticketRepository.findSummaryByDepartmentIdAndStatus(departmentId, status);
        }
        return ticketRepository.findSummaryByDepartmentId(departmentId);
    }

    public List<TicketSummaryDto> getAllTicketSummaries(String status) {
        if ("OVERDUE".equalsIgnoreCase(status)) {
            return ticketRepository.findAllSummaries().stream()
                    .filter(TicketSummaryDto::isOverdue)
                    .toList();
        }
        if (status != null && !status.trim().isEmpty() && !"ALL".equalsIgnoreCase(status)) {
            return ticketRepository.findAllSummariesByStatus(status);
        }
        return ticketRepository.findAllSummaries();
    }

    public List<TicketSummaryDto> getMyTicketSummaries(Long userId) {
        if (userId == null) {
            return Collections.emptyList();
        }
        return ticketRepository.findSummaryByCreatorId(userId);
    }

    /**
     * Lấy danh sách Ticket phân trang của sinh viên với các bộ lọc: trạng thái, đơn vị, từ khóa
     */
    public Page<TicketResponseDto> getMyTicketsPaged(Long userId, String status, Long departmentId, String keyword, Pageable pageable) {
        if (userId == null) {
            return Page.empty(pageable);
        }
        String normalizedStatus = (status == null || status.isBlank() || "ALL".equalsIgnoreCase(status)) ? null : status.toUpperCase();
        String normalizedKeyword = (keyword == null || keyword.isBlank()) ? null : keyword.trim();
        Page<Ticket> page = ticketRepository.findMyTicketsPaged(userId, normalizedStatus, departmentId, normalizedKeyword, pageable);
        return page.map(this::mapToSummaryResponseDto);
    }

    /**
     * Đếm số lượng ticket của sinh viên theo các trạng thái phục vụ hiển thị Badges trên Tab
     */
    @Cacheable(value = "user_ticket_counts", key = "#userId", unless = "#userId == null")
    public Map<String, Long> getMyTicketStatusCounts(Long userId) {
        Map<String, Long> counts = new HashMap<>();
        if (userId == null) {
            counts.put("total", 0L);
            counts.put("pending", 0L);
            counts.put("processing", 0L);
            counts.put("waiting", 0L);
            counts.put("resolved", 0L);
            counts.put("closed", 0L);
            return counts;
        }
        counts.put("total", ticketRepository.countMyTicketsTotal(userId));
        counts.put("pending", ticketRepository.countMyTicketsPending(userId));
        counts.put("processing", ticketRepository.countMyTicketsProcessing(userId));
        counts.put("waiting", ticketRepository.countMyTicketsWaiting(userId));
        counts.put("resolved", ticketRepository.countMyTicketsResolved(userId));
        counts.put("closed", ticketRepository.countMyTicketsClosed(userId));
        return counts;
    }

    /**
     * Lấy danh sách các đơn vị/khoa đang hoạt động dạng DTO chuẩn MVC
     */
    public List<DepartmentAdminDto> getActiveDepartments() {
        return departmentRepository.findByIsActiveTrue().stream()
                .map(d -> DepartmentAdminDto.builder()
                        .id(d.getId())
                        .name(d.getName())
                        .code(d.getCode())
                        .officeLocation(d.getOfficeLocation())
                        .contactEmail(d.getContactEmail())
                        .contactPhone(d.getContactPhone())
                        .description(d.getDescription())
                        .isActive(d.getIsActive())
                        .build())
                .toList();
    }

    /**
     * Xử lý thống kê trạng thái và phân trang cho Bàn làm việc Cán bộ (Staff Dashboard)
     */
    public StaffDashboardDataDto getStaffDashboardData(Long departmentId, String status, int page, int size) {
        List<TicketSummaryDto> allTickets;
        if (departmentId != null) {
            allTickets = getTicketSummaryByDepartment(departmentId, "ALL");
        } else {
            allTickets = getAllTicketSummaries("ALL");
        }

        long countOpen = allTickets.stream().filter(t -> "OPEN".equalsIgnoreCase(t.getStatus())).count();
        long countInProgress = allTickets.stream().filter(t -> "IN_PROGRESS".equalsIgnoreCase(t.getStatus())).count();
        long countOverdue = allTickets.stream().filter(TicketSummaryDto::isOverdue).count();
        long countResolved = allTickets.stream().filter(t -> "RESOLVED".equalsIgnoreCase(t.getStatus()) || "CLOSED".equalsIgnoreCase(t.getStatus())).count();
        long countNewReplies = allTickets.stream().filter(TicketSummaryDto::getHasStudentReply).count();

        List<TicketSummaryDto> filteredTickets;
        if ("ALL".equalsIgnoreCase(status) || status == null || status.isBlank()) {
            filteredTickets = allTickets;
        } else if ("NEW_REPLY".equalsIgnoreCase(status)) {
            filteredTickets = allTickets.stream().filter(TicketSummaryDto::getHasStudentReply).toList();
        } else if ("OVERDUE".equalsIgnoreCase(status)) {
            filteredTickets = allTickets.stream().filter(TicketSummaryDto::isOverdue).toList();
        } else {
            filteredTickets = allTickets.stream().filter(t -> status.equalsIgnoreCase(t.getStatus())).toList();
        }

        int totalElements = filteredTickets.size();
        int safePage = Math.max(0, page);
        int safeSize = Math.max(1, size);
        int fromIndex = Math.min(safePage * safeSize, totalElements);
        int toIndex = Math.min(fromIndex + safeSize, totalElements);
        List<TicketSummaryDto> pagedContent = filteredTickets.subList(fromIndex, toIndex);

        Page<TicketSummaryDto> ticketPage =
                new PageImpl<>(pagedContent, PageRequest.of(safePage, safeSize), totalElements);

        return StaffDashboardDataDto.builder()
                .tickets(ticketPage.getContent())
                .ticketPage(ticketPage)
                .currentPage(safePage)
                .totalPages(ticketPage.getTotalPages())
                .totalElements(totalElements)
                .countOpen(countOpen)
                .countInProgress(countInProgress)
                .countOverdue(countOverdue)
                .countResolved(countResolved)
                .countNewReplies(countNewReplies)
                .build();
    }

    /**
     * Map gọn phục vụ hiển thị danh sách (không tải lịch sử hay tệp đính kèm nặng)
     */
    private TicketResponseDto mapToSummaryResponseDto(Ticket t) {
        boolean isOverdue = slaCalculatorService.isOverdue(t.getDueDate(), t.getStatus());
        boolean isDueSoon = slaCalculatorService.isDueSoon(t.getDueDate(), t.getStatus());

        return TicketResponseDto.builder()
                .id(t.getId())
                .ticketCode(t.getTicketCode())
                .title(t.getTitle())
                .description(t.getDescription())
                .departmentId(t.getDepartment() != null ? t.getDepartment().getId() : null)
                .departmentName(t.getDepartment() != null ? t.getDepartment().getName() : "")
                .creatorId(t.getCreator() != null ? t.getCreator().getId() : null)
                .creatorName(t.getCreator() != null ? t.getCreator().getFullName() : (t.getGuestName() != null ? t.getGuestName() : ""))
                .assignedStaffId(t.getAssignedTo() != null ? t.getAssignedTo().getId() : null)
                .assignedStaffName(t.getAssignedTo() != null ? t.getAssignedTo().getFullName() : null)
                .priority(t.getPriority())
                .topicCategory(t.getTopicCategory())
                .status(t.getStatus())
                .dueDate(t.getDueDate())
                .isOverdue(isOverdue)
                .isDueSoon(isDueSoon)
                .createdAt(t.getCreatedAt())
                .updatedAt(t.getUpdatedAt())
                .resolvedAt(t.getResolvedAt())
                .closedAt(t.getClosedAt())
                .rating(t.getRating())
                .build();
    }

    @Transactional
    public void markStaffSeen(Long ticketId, Long staffId) {
        ticketRepository.findById(ticketId).ifPresent(t -> {
            if (Boolean.TRUE.equals(t.getHasStudentReply())) {
                t.setHasStudentReply(false);
                ticketRepository.save(t);
            }
        });
    }

    public long countTicketsWithStudentReply(Long departmentId) {
        if (departmentId == null) {
            return ticketRepository.countAllByHasStudentReplyTrue();
        }
        return ticketRepository.countByDepartmentIdAndHasStudentReplyTrue(departmentId);
    }

    // ─── PRIVATE HELPERS ────────────────────────────────────────────────────

    private String buildTrackingUrl(Ticket ticket) {
        String baseUrl = (serverBaseUrl != null && !serverBaseUrl.isBlank()) ? serverBaseUrl.trim() : "http://localhost:8080";
        if (baseUrl.endsWith("/")) {
            baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        }
        if (ticket.getGuestToken() != null) {
            return baseUrl + "/tickets/guest-track?token=" + ticket.getGuestToken();
        }
        return baseUrl + "/tickets/detail/" + ticket.getId();
    }

    private void saveAttachments(Ticket ticket, TicketHistory history, List<MultipartFile> files, String folder) {
        if (files == null || files.isEmpty()) return;
        for (MultipartFile file : files) {
            if (file != null && !file.isEmpty()) {
                IStorageService.StorageResult res = storageService.uploadFile(file, folder);
                Attachment attachment = Attachment.builder()
                        .fileName(res.originalFileName())
                        .fileUrl(res.publicUrl())
                        .fileType(res.fileType())
                        .fileSize(res.fileSizeBytes())
                        .ticket(ticket)
                        .ticketHistory(history)
                        .build();
                attachmentRepository.save(attachment);
            }
        }
    }

    private void validateTicketAccess(Long ticketId, User staff) {
        if (staff.getRole() != null && "ROLE_ADMIN".equalsIgnoreCase(staff.getRole().getName())) return;
        ticketRepository.findAccessAuthInfoById(ticketId).ifPresent(authInfo -> {
            Long staffDeptId = staff.getDepartment() != null ? staff.getDepartment().getId() : null;
            if (!Objects.equals(authInfo.getDepartmentId(), staffDeptId)) {
                throw new AccessDeniedBusinessException(
                        "Cán bộ chỉ được tiếp nhận Ticket thuộc đơn vị của mình!");
            }
        });
    }

    private TicketHistory saveHistory(Ticket ticket, User actor, String actorName,
                                      String fromStatus, String toStatus, String note) {
        TicketHistory history = TicketHistory.builder()
                .ticket(ticket)
                .actor(actor)
                .actorName(actorName)
                .fromStatus(fromStatus)
                .toStatus(toStatus)
                .actionNote(note)
                .build();
        return ticketHistoryRepository.save(history);
    }

    private String generateTicketCode() {
        String dateStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String randomSuffix = UUID.randomUUID().toString().substring(0, 4).toUpperCase();
        return "TK-" + dateStr + "-" + randomSuffix;
    }

    private TicketResponseDto mapToDetailDto(Ticket t) {
        List<Attachment> allAttachments = attachmentRepository.findByTicketId(t.getId());

        List<AttachmentDto> attachments = allAttachments.stream()
                .map(a -> AttachmentDto.builder()
                        .id(a.getId())
                        .fileName(a.getFileName())
                        .fileUrl(a.getFileUrl())
                        .fileType(a.getFileType())
                        .fileSize(a.getFileSize())
                        .build())
                .collect(Collectors.toList());

        List<TicketHistory> historyEntities = ticketHistoryRepository
                .findByTicketIdOrderByCreatedAtAsc(t.getId());

        List<HistoryDto> histories = historyEntities.stream()
                .filter(h -> {
                    if (h.getFromStatus() == null && h.getActionNote() != null
                            && h.getActionNote().startsWith("Khởi tạo yêu cầu")) {
                        return false;
                    }
                    return true;
                })
                .map(h -> {
                    List<AttachmentDto> matchedAtts = allAttachments.stream()
                            .filter(a -> a.getTicketHistory() != null && a.getTicketHistory().getId().equals(h.getId()))
                            .map(a -> AttachmentDto.builder()
                                    .id(a.getId())
                                    .fileName(a.getFileName())
                                    .fileUrl(a.getFileUrl())
                                    .fileType(a.getFileType())
                                    .fileSize(a.getFileSize())
                                    .build())
                            .collect(Collectors.toList());

                    return HistoryDto.builder()
                            .id(h.getId())
                            .actorId(h.getActor() != null ? h.getActor().getId() : null)
                            .actorRole(h.getActor() != null && h.getActor().getRole() != null ? h.getActor().getRole().getName() : null)
                            .actorName(h.getActorName())
                            .fromStatus(h.getFromStatus())
                            .toStatus(h.getToStatus())
                            .actionNote(h.getActionNote())
                            .createdAt(h.getCreatedAt())
                            .attachments(matchedAtts)
                            .build();
                })
                .collect(Collectors.toList());

        Set<Long> replyAttachmentIds = histories.stream()
                .flatMap(h -> h.getAttachments().stream())
                .map(AttachmentDto::getId)
                .collect(Collectors.toSet());

        List<AttachmentDto> initialAttachments = attachments.stream()
                .filter(a -> !replyAttachmentIds.contains(a.getId()))
                .collect(Collectors.toList());

        boolean isOverdue = slaCalculatorService.isOverdue(t.getDueDate(), t.getStatus());
        boolean isDueSoon = slaCalculatorService.isDueSoon(t.getDueDate(), t.getStatus());

        List<TicketTransferResponse> transferResponses = ticketTransferHistoryRepository
                .findByTicketIdWithRelations(t.getId())
                .stream()
                .map(tth -> TicketTransferResponse.builder()
                        .id(tth.getId())
                        .ticketId(t.getId())
                        .ticketCode(t.getTicketCode())
                        .fromDepartmentId(tth.getFromDepartment() != null ? tth.getFromDepartment().getId() : null)
                        .fromDepartmentName(tth.getFromDepartment() != null ? tth.getFromDepartment().getName() : null)
                        .toDepartmentId(tth.getToDepartment() != null ? tth.getToDepartment().getId() : null)
                        .toDepartmentName(tth.getToDepartment() != null ? tth.getToDepartment().getName() : null)
                        .transferredById(tth.getTransferredBy() != null ? tth.getTransferredBy().getId() : null)
                        .transferredByName(tth.getTransferredBy() != null ? tth.getTransferredBy().getFullName() : null)
                        .reason(tth.getReason())
                        .transferredAt(tth.getCreatedAt())
                        .build())
                .collect(Collectors.toList());

        return TicketResponseDto.builder()
                .id(t.getId())
                .ticketCode(t.getTicketCode())
                .title(t.getTitle())
                .description(t.getDescription())
                .departmentId(t.getDepartment().getId())
                .departmentName(t.getDepartment().getName())
                .creatorId(t.getCreator() != null ? t.getCreator().getId() : null)
                .creatorName(t.getCreator() != null ? t.getCreator().getFullName() : null)
                .guestName(t.getGuestName())
                .guestEmail(t.getGuestEmail())
                .guestToken(t.getGuestToken())
                .assignedStaffId(t.getAssignedTo() != null ? t.getAssignedTo().getId() : null)
                .assignedStaffName(t.getAssignedTo() != null ? t.getAssignedTo().getFullName() : null)
                .assignedStaffEmail(t.getAssignedTo() != null ? t.getAssignedTo().getEmail() : null)
                .assignedStaffPhone(t.getAssignedTo() != null ? t.getAssignedTo().getPhone() : null)
                .assignedStaffAvatarUrl(t.getAssignedTo() != null ? t.getAssignedTo().getAvatarUrl() : null)
                .assignedStaffRole(t.getAssignedTo() != null && t.getAssignedTo().getRole() != null ? t.getAssignedTo().getRole().getName() : null)
                .priority(t.getPriority())
                .topicCategory(t.getTopicCategory())
                .status(t.getStatus())
                .dueDate(t.getDueDate())
                .isOverdue(isOverdue)
                .isDueSoon(isDueSoon)
                .createdAt(t.getCreatedAt())
                .updatedAt(t.getUpdatedAt())
                .resolvedAt(t.getResolvedAt())
                .closedAt(t.getClosedAt())
                .rating(t.getRating())
                .attachments(attachments)
                .initialAttachments(initialAttachments)
                .histories(histories)
                .transferHistories(transferResponses)
                .build();
    }
}