package com.school.counseling.module.admin.controller;

import com.school.counseling.common.dto.ApiResponse;
import com.school.counseling.common.util.SecurityUtils;
import com.school.counseling.module.admin.dto.InternalMessageDto;
import com.school.counseling.module.admin.dto.SendInternalMessageRequest;
import com.school.counseling.module.admin.service.InternalMessageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/internal-messages")
@PreAuthorize("hasAnyRole('STAFF', 'ADMIN')")
@RequiredArgsConstructor
public class InternalMessageRestController {

    private final InternalMessageService internalMessageService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<InternalMessageDto>>> getMessages(
            @RequestParam(value = "departmentId", required = false) Long departmentId) {

        // [USER_REQUEST]: Mặc định lấy tất cả trao đổi của toàn thể cán bộ & admin, không giới hạn theo phòng
        List<InternalMessageDto> messages = internalMessageService.getMessages(departmentId);
        return ResponseEntity.ok(ApiResponse.success(messages, "Tải danh sách trao đổi nội bộ thành công"));
    }

    @PostMapping("/send")
    public ResponseEntity<ApiResponse<InternalMessageDto>> sendMessage(
            @Valid @RequestBody SendInternalMessageRequest request) {

        Long senderId = SecurityUtils.getCurrentUserId()
                .orElseThrow(() -> new IllegalStateException("Không tìm thấy thông tin tài khoản đăng nhập"));

        InternalMessageDto result = internalMessageService.sendMessage(senderId, request);
        return ResponseEntity.ok(ApiResponse.success(result, "Gửi tin nhắn trao đổi nội bộ thành công"));
    }

    @PostMapping("/{id}/read")
    public ResponseEntity<ApiResponse<Void>> markAsRead(@PathVariable Long id) {
        internalMessageService.markAsRead(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Đã đánh dấu đọc tin nhắn"));
    }
}
