package com.school.counseling.module.ticket.service;

import com.school.counseling.common.exception.AccessDeniedBusinessException;
import com.school.counseling.common.exception.ResourceNotFoundException;
import com.school.counseling.common.exception.TicketAlreadyClaimedException;
import com.school.counseling.common.storage.IStorageService;
import com.school.counseling.module.auth.entity.Department;
import com.school.counseling.module.auth.entity.User;
import com.school.counseling.module.auth.repository.DepartmentRepository;
import com.school.counseling.module.auth.repository.UserRepository;
import com.school.counseling.module.chat.entity.Attachment;
import com.school.counseling.module.chat.repository.AttachmentRepository;
import com.school.counseling.module.ticket.dto.*;
import com.school.counseling.module.ticket.dto.TicketResponseDto.AttachmentDto;
import com.school.counseling.module.ticket.dto.TicketResponseDto.HistoryDto;
import com.school.counseling.module.ticket.entity.Ticket;
import com.school.counseling.module.ticket.entity.TicketHistory;
import com.school.counseling.module.ticket.event.TicketClaimedEvent;
import com.school.counseling.module.ticket.event.TicketCreatedEvent;
import com.school.counseling.module.ticket.event.TicketResolvedEvent;
import com.school.counseling.module.ticket.repository.TicketHistoryRepository;
import com.school.counseling.module.ticket.repository.TicketRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

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
            if (request.getGuestEmail() == null || request.getGuestEmail().isBlank()) {
                throw new IllegalArgumentException("Vui lòng nhập địa chỉ email hợp lệ để nhận mã tra cứu và thông báo xử lý yêu cầu.");
            }
            if (request.getGuestName() == null || request.getGuestName().isBlank()) {
                request.setGuestName("Khách vãng lai");
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
    public TicketResponseDto resolveTicket(Long ticketId, Long staffId, String resolutionNote) {
        Ticket ticket = ticketRepository.findByIdWithRelations(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket", "id", ticketId));

        User staff = userRepository.findById(staffId).orElse(null);
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
        if (status != null && !status.trim().isEmpty() && !"ALL".equalsIgnoreCase(status)) {
            return ticketRepository.findSummaryByDepartmentIdAndStatus(departmentId, status);
        }
        return ticketRepository.findSummaryByDepartmentId(departmentId);
    }

    public List<TicketSummaryDto> getAllTicketSummaries(String status) {
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
     * Map gọn phục vụ hiển thị danh sách (không tải lịch sử hay tệp đính kèm nặng)
     */
    public TicketResponseDto mapToSummaryResponseDto(Ticket t) {
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
                .build();
    }
}