package com.school.counseling.common;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.ModelAndView;

import java.util.HashMap;
import java.util.Map;

/**
 * Xử lý lỗi toàn cục (Global Exception Handler) cho QAUTE Portal.
 * Hỗ trợ linh hoạt cả API REST JSON và Giao diện Web Thymeleaf.
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * Kiểm tra xem request hiện tại là yêu cầu trả về JSON (AJAX / REST) hay trang web thông thường.
     */
    private boolean isApiRequest(HttpServletRequest request) {
        String acceptHeader = request.getHeader("Accept");
        String uri = request.getRequestURI();
        return (acceptHeader != null && acceptHeader.contains("application/json")) || uri.startsWith("/api/");
    }

    /**
     * Bắt lỗi nghiệp vụ tuỳ chỉnh (AppException).
     */
    @ExceptionHandler(AppException.class)
    public Object handleAppException(AppException ex, HttpServletRequest request) {
        log.warn("Ngoại lệ nghiệp vụ AppException [{}]: {}", ex.getStatusCode(), ex.getMessage());

        if (isApiRequest(request)) {
            return ResponseEntity
                    .status(ex.getStatusCode())
                    .body(ApiResponse.error(ex.getStatusCode(), ex.getMessage()));
        }

        ModelAndView mav = new ModelAndView("error");
        mav.addObject("errorCode", ex.getStatusCode());
        mav.addObject("errorMessage", ex.getMessage());
        return mav;
    }

    /**
     * Bắt lỗi dữ liệu đầu vào không hợp lệ khi dùng @Valid (MethodArgumentNotValidException).
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseBody
    public ResponseEntity<ApiResponse<Map<String, String>>> handleValidationException(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            errors.put(error.getField(), error.getDefaultMessage());
        }

        log.warn("Lỗi validation dữ liệu đầu vào: {}", errors);
        return ResponseEntity
                .badRequest()
                .body(ApiResponse.error(HttpStatus.BAD_REQUEST.value(), "Dữ liệu gửi lên không hợp lệ!", errors));
    }

    /**
     * Bắt lỗi vượt quá dung lượng tệp tin tải lên (MaxUploadSizeExceededException).
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public Object handleMaxSizeException(MaxUploadSizeExceededException ex, HttpServletRequest request) {
        String message = "Dung lượng tệp tin tải lên vượt quá giới hạn cho phép!";
        log.warn(message);

        if (isApiRequest(request)) {
            return ResponseEntity
                    .status(HttpStatus.PAYLOAD_TOO_LARGE)
                    .body(ApiResponse.error(HttpStatus.PAYLOAD_TOO_LARGE.value(), message));
        }

        ModelAndView mav = new ModelAndView("error");
        mav.addObject("errorCode", 413);
        mav.addObject("errorMessage", message);
        return mav;
    }

    /**
     * Bắt lỗi tham số không hợp lệ (IllegalArgumentException).
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public Object handleIllegalArgumentException(IllegalArgumentException ex, HttpServletRequest request) {
        log.warn("Tham số không hợp lệ: {}", ex.getMessage());

        if (isApiRequest(request)) {
            return ResponseEntity
                    .badRequest()
                    .body(ApiResponse.error(HttpStatus.BAD_REQUEST.value(), ex.getMessage()));
        }

        ModelAndView mav = new ModelAndView("error");
        mav.addObject("errorCode", 400);
        mav.addObject("errorMessage", ex.getMessage());
        return mav;
    }

    /**
     * Bắt các lỗi hệ thống không lường trước (500 Internal Server Error).
     */
    @ExceptionHandler(Exception.class)
    public Object handleGeneralException(Exception ex, HttpServletRequest request) {
        log.error("Lỗi hệ thống không lường trước: ", ex);
        String message = "Đã xảy ra lỗi nội bộ máy chủ. Vui lòng thử lại sau!";

        if (isApiRequest(request)) {
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error(HttpStatus.INTERNAL_SERVER_ERROR.value(), message));
        }

        ModelAndView mav = new ModelAndView("error");
        mav.addObject("errorCode", 500);
        mav.addObject("errorMessage", message);
        return mav;
    }
}
