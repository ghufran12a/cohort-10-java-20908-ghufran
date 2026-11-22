package com.contactapp.contactmanager.exception;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * The ONE error shape the frontend always receives, whatever went wrong:
 * { "timestamp": ..., "status": 400, "error": "Bad Request", "message": "...", "path": "/api/...",
 *   "fieldErrors": { "phones[0].phone": "Phone must contain only digits..." } }
 */
@JsonInclude(JsonInclude.Include.NON_NULL) // leave out fieldErrors when there are none
public record ApiError(
        LocalDateTime timestamp,
        int status,
        String error,
        String message,
        String path,
        Map<String, String> fieldErrors) {
}
