package com.project.shopapp.exceptions;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(org.springframework.http.converter.HttpMessageNotReadableException.class)
    ResponseEntity<ApiError> unreadableRequest(Exception exception) {
        return build(HttpStatus.BAD_REQUEST, "Invalid request JSON or enum value", Map.of());
    }

    @ExceptionHandler(org.springframework.orm.ObjectOptimisticLockingFailureException.class)
    ResponseEntity<ApiError> concurrentUpdate(Exception exception) {
        return build(HttpStatus.CONFLICT, "Data changed concurrently. Reload and retry", Map.of());
    }

    @ExceptionHandler(org.springframework.web.multipart.MaxUploadSizeExceededException.class)
    ResponseEntity<ApiError> uploadTooLarge(Exception exception) {
        return build(
                HttpStatus.PAYLOAD_TOO_LARGE, "Image upload exceeds the allowed size", Map.of());
    }

    @ExceptionHandler(java.io.IOException.class)
    ResponseEntity<ApiError> storageFailure(java.io.IOException exception) {
        org.slf4j.LoggerFactory.getLogger(GlobalExceptionHandler.class)
                .error("Image storage operation failed", exception);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "Image storage is unavailable", Map.of());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ResponseEntity<ApiError> invalidArgument(IllegalArgumentException exception) {
        return build(HttpStatus.BAD_REQUEST, exception.getMessage(), Map.of());
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    ResponseEntity<ApiError> notFound(ResourceNotFoundException exception) {
        return build(HttpStatus.NOT_FOUND, exception.getMessage(), Map.of());
    }

    @ExceptionHandler(DuplicateResourceException.class)
    ResponseEntity<ApiError> conflict(DuplicateResourceException exception) {
        return build(HttpStatus.CONFLICT, exception.getMessage(), Map.of());
    }

    @ExceptionHandler(InsufficientStockException.class)
    ResponseEntity<ApiError> insufficientStock(InsufficientStockException exception) {
        return build(HttpStatus.CONFLICT, exception.getMessage(), Map.of());
    }

    @ExceptionHandler(IllegalStateException.class)
    ResponseEntity<ApiError> invalidState(IllegalStateException exception) {
        return build(HttpStatus.CONFLICT, exception.getMessage(), Map.of());
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    ResponseEntity<ApiError> unauthorized(InvalidCredentialsException exception) {
        return build(HttpStatus.UNAUTHORIZED, exception.getMessage(), Map.of());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiError> validation(MethodArgumentNotValidException exception) {
        Map<String, String> errors = new LinkedHashMap<>();
        exception
                .getBindingResult()
                .getFieldErrors()
                .forEach(error -> errors.putIfAbsent(error.getField(), error.getDefaultMessage()));
        return build(HttpStatus.BAD_REQUEST, "Request validation failed", errors);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<ApiError> dataIntegrity(DataIntegrityViolationException exception) {
        return build(
                HttpStatus.CONFLICT,
                "The resource is referenced by other data or violates a unique constraint",
                Map.of());
    }

    private ResponseEntity<ApiError> build(
            HttpStatus status, String message, Map<String, String> errors) {
        return ResponseEntity.status(status)
                .body(new ApiError(Instant.now(), status.value(), message, errors));
    }
}
