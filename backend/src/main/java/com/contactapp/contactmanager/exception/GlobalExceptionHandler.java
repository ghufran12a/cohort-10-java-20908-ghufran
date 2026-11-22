package com.contactapp.contactmanager.exception;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/**
 * One central place that catches exceptions from ALL controllers and turns them into
 * clean JSON errors. Think of it as a safety net under every controller method.
 *
 * Without it, an exception becomes an ugly 500 with no useful message (we saw this in Phase 6).
 * Rule of thumb for logging: 4xx = the client's mistake -> WARN (short);
 *                            5xx = OUR bug -> ERROR (with the full stack trace).
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // Something does not exist (or is not the user's) -> 404
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiError> handleNotFound(ResourceNotFoundException ex, HttpServletRequest request) {
        log.warn("Not found: {} [{} {}]", ex.getMessage(), request.getMethod(), request.getRequestURI());
        return build(HttpStatus.NOT_FOUND, ex.getMessage(), request, null);
    }

    // @Valid failed (bad phone, bad email, empty name...) -> 400 with one message per bad field
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(fe -> fieldErrors.putIfAbsent(fe.getField(), fe.getDefaultMessage()));
        log.warn("Validation failed [{} {}]: {}", request.getMethod(), request.getRequestURI(), fieldErrors);
        return build(HttpStatus.BAD_REQUEST, "Validation failed", request, fieldErrors);
    }

    // The JSON itself is broken (missing bracket, wrong type...) -> 400
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> handleUnreadable(HttpMessageNotReadableException ex, HttpServletRequest request) {
        log.warn("Malformed request body [{} {}]", request.getMethod(), request.getRequestURI());
        return build(HttpStatus.BAD_REQUEST, "Request body is missing or is not valid JSON", request, null);
    }

    // e.g. /api/contacts/abc where a number was expected -> 400
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiError> handleTypeMismatch(MethodArgumentTypeMismatchException ex, HttpServletRequest request) {
        log.warn("Bad parameter '{}' [{} {}]", ex.getName(), request.getMethod(), request.getRequestURI());
        return build(HttpStatus.BAD_REQUEST, "Parameter '" + ex.getName() + "' has an invalid value", request, null);
    }

    // Database rule broken (e.g. duplicate email/phone) -> 409 Conflict
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiError> handleIntegrity(DataIntegrityViolationException ex, HttpServletRequest request) {
        log.warn("Data integrity violation [{} {}]: {}", request.getMethod(), request.getRequestURI(),
                ex.getMostSpecificCause().getMessage());
        return build(HttpStatus.CONFLICT, "This data conflicts with existing data (for example a duplicate)", request, null);
    }

    // Safety net for everything else. Includes Spring's own web errors (unknown url -> 404,
    // wrong HTTP method -> 405...), which already know their own status code.
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleAny(Exception ex, HttpServletRequest request) {
        if (ex instanceof ErrorResponse errorResponse) {
            HttpStatusCode status = errorResponse.getStatusCode();
            log.warn("{} [{} {}]: {}", status, request.getMethod(), request.getRequestURI(), ex.getMessage());
            return build(status, errorResponse.getBody().getDetail(), request, null);
        }
        // A real bug: log everything for us, but tell the client nothing about our internals
        log.error("Unexpected error [{} {}]", request.getMethod(), request.getRequestURI(), ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "Something went wrong. Please try again later.", request, null);
    }

    private ResponseEntity<ApiError> build(HttpStatusCode status, String message,
                                           HttpServletRequest request, Map<String, String> fieldErrors) {
        String reason = (status instanceof HttpStatus hs) ? hs.getReasonPhrase() : status.toString();
        ApiError body = new ApiError(LocalDateTime.now(), status.value(), reason, message,
                request.getRequestURI(), fieldErrors);
        return ResponseEntity.status(status).body(body);
    }
}
