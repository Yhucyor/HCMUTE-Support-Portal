package com.school.counseling.common.exception;

import com.school.counseling.common.dto.ApiResponse;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Điều hướng và chuẩn hóa xử lý lỗi hệ thống (/error)
 * Tự động phân loại:
 * - Trả JSON (ApiResponse<Void>) cho các yêu cầu gọi REST API (/api/...)
 * - Trả giao diện Thymeleaf (error.html) cho các truy cập trình duyệt người dùng
 */
@Slf4j
@Controller
public class CustomErrorController implements ErrorController {

    @RequestMapping("/error")
    public Object handleError(HttpServletRequest request, Model model) {
        Object statusObj = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        Object messageObj = request.getAttribute(RequestDispatcher.ERROR_MESSAGE);
        String requestUri = (String) request.getAttribute(RequestDispatcher.ERROR_REQUEST_URI);
        if (requestUri == null) {
            requestUri = request.getRequestURI();
        }

        int statusCode = HttpStatus.INTERNAL_SERVER_ERROR.value();
        if (statusObj != null) {
            try {
                statusCode = Integer.parseInt(statusObj.toString());
            } catch (NumberFormatException ignored) {}
        }

        String userFriendlyMsg = switch (statusCode) {
            case 400 -> "Yêu cầu không hợp lệ. Vui lòng kiểm tra lại thông tin gửi đi.";
            case 401 -> "Bạn chưa đăng nhập hoặc phiên làm việc đã hết hạn. Vui lòng đăng nhập lại.";
            case 403 -> "Bạn không có quyền truy cập vào tài nguyên hoặc khu vực này.";
            case 404 -> "Trang hoặc tài nguyên bạn đang tìm kiếm không tồn tại hoặc đã bị xóa.";
            case 405 -> "Phương thức HTTP không được hỗ trợ trên địa chỉ này.";
            case 413 -> "Tệp tải lên vượt quá dung lượng tối đa cho phép.";
            case 500 -> "Hệ thống đang gặp sự cố xử lý nội bộ. Đội ngũ kỹ thuật đang kiểm tra.";
            default -> (messageObj != null && !messageObj.toString().isBlank()) 
                    ? messageObj.toString() 
                    : "Đã xảy ra lỗi không xác định (" + statusCode + ").";
        };

        log.warn("Đã xử lý mã lỗi HTTP {}: URI={}, Detail={}", statusCode, requestUri, userFriendlyMsg);

        // Kiểm tra nếu là lời gọi API RESTful
        String acceptHeader = request.getHeader("Accept");
        boolean isApiRequest = (requestUri != null && requestUri.startsWith("/api/"))
                || (acceptHeader != null && acceptHeader.contains(MediaType.APPLICATION_JSON_VALUE));

        if (isApiRequest) {
            return ResponseEntity.status(statusCode)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(ApiResponse.<Void>error(userFriendlyMsg));
        }

        // Với giao diện Web Thymeleaf: nạp dữ liệu vào Model
        model.addAttribute("errorCode", String.valueOf(statusCode));
        model.addAttribute("errorMessage", userFriendlyMsg);
        return "error";
    }
}
