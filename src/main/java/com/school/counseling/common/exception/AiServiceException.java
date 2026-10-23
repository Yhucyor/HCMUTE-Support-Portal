package com.school.counseling.common.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Exception khi AI Service gặp lỗi
 * HTTP Status: 503 Service Unavailable
 */
@ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
public class AiServiceException extends RuntimeException {
    
    public AiServiceException(String message) {
        super(message);
    }
    
    public AiServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}
