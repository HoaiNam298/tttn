package com.project.shopapp.exceptions;

import java.io.IOException;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.ConversionNotSupportedException;
import org.springframework.beans.TypeMismatchException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<ApiError> unreadableRequest(Exception exception) {
        return build(HttpStatus.BAD_REQUEST, "Invalid request JSON or enum value", Map.of());
    }

    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    ResponseEntity<ApiError> concurrentUpdate(Exception exception) {
        return build(HttpStatus.CONFLICT, "Data changed concurrently. Reload and retry", Map.of());
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    ResponseEntity<ApiError> uploadTooLarge(Exception exception) {
        return build(
                HttpStatus.PAYLOAD_TOO_LARGE, "Image upload exceeds the allowed size", Map.of());
    }

    @ExceptionHandler(IOException.class)
    ResponseEntity<ApiError> storageFailure(IOException exception) {
        log.error("Image storage operation failed", exception);
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
        var fieldErrors = exception.getBindingResult().getFieldErrors();
        for (var error : fieldErrors) {
            errors.putIfAbsent(error.getField(), error.getDefaultMessage());
        }
        return build(HttpStatus.BAD_REQUEST, "Request validation failed", errors);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<ApiError> dataIntegrity(DataIntegrityViolationException exception) {
        return build(
                HttpStatus.CONFLICT,
                "The resource is referenced by other data or violates a unique constraint",
                Map.of());
    }

    @ExceptionHandler(AccessDeniedException.class)
    ResponseEntity<ApiError> accessDenied(AccessDeniedException exception) {
        return build(HttpStatus.FORBIDDEN, "Access denied", Map.of());
    }

    @ExceptionHandler(AuthenticationException.class)
    ResponseEntity<ApiError> authenticationFailure(AuthenticationException exception) {
        if (exception instanceof AuthenticationServiceException) {
            log.error("Authentication failed due to a system error", exception);
            return build(
                    HttpStatus.INTERNAL_SERVER_ERROR, "Authentication is unavailable", Map.of());
        }
        return build(HttpStatus.UNAUTHORIZED, "Authentication required", Map.of());
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ApiError> unexpectedFailure(Exception exception) {
        if (exception instanceof TypeMismatchException
                && !(exception instanceof ConversionNotSupportedException)) {
            return build(HttpStatus.BAD_REQUEST, "Invalid request parameter", Map.of());
        }
        if (exception instanceof ErrorResponse error) {
            var status = error.getStatusCode();
            if (status.is4xxClientError()) {
                return ResponseEntity.status(status)
                        .headers(error.getHeaders())
                        .body(
                                new ApiError(
                                        Instant.now(),
                                        status.value(),
                                        "Request rejected",
                                        Map.of()));
            }
        }
        log.error("Unexpected request processing failure", exception);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred", Map.of());
    }

    private ResponseEntity<ApiError> build(
            HttpStatus status, String message, Map<String, String> errors) {
        return ResponseEntity.status(status)
                .body(new ApiError(Instant.now(), status.value(), message, errors));
    }
}
