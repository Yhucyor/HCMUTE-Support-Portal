package com.school.counseling.common.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Exception khi user vượt quá rate limit
 * HTTP Status: 429 Too Many Requests
 */
@ResponseStatus(HttpStatus.TOO_MANY_REQUESTS)
public class RateLimitExceededException extends RuntimeException {
    
    public RateLimitExceededException(String message) {
        super(message);
    }
    
    public RateLimitExceededException(String message, Throwable cause) {
        super(message, cause);
    }
}
