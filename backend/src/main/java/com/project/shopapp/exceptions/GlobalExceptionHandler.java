package com.project.shopapp.exceptions;

import com.project.shopapp.components.I18nMessages;
import com.project.shopapp.components.MessageKeys;
import java.io.IOException;
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
    private final I18nMessages messages;

    public GlobalExceptionHandler(I18nMessages messages) {
        this.messages = messages;
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<ApiError> unreadableRequest(Exception exception) {
        return build(HttpStatus.BAD_REQUEST, MessageKeys.INVALID_REQUEST, Map.of());
    }

    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    ResponseEntity<ApiError> concurrentUpdate(Exception exception) {
        return build(HttpStatus.CONFLICT, MessageKeys.REQUEST_CONCURRENT_UPDATE, Map.of());
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    ResponseEntity<ApiError> uploadTooLarge(Exception exception) {
        return build(HttpStatus.PAYLOAD_TOO_LARGE, MessageKeys.UPLOAD_TOO_LARGE, Map.of());
    }

    @ExceptionHandler(IOException.class)
    ResponseEntity<ApiError> storageFailure(IOException exception) {
        log.error("Image storage operation failed", exception);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, MessageKeys.STORAGE_UNAVAILABLE, Map.of());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ResponseEntity<ApiError> invalidArgument(IllegalArgumentException exception) {
        return business(HttpStatus.BAD_REQUEST, exception, MessageKeys.REQUEST_INVALID_ARGUMENT);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    ResponseEntity<ApiError> notFound(ResourceNotFoundException exception) {
        return business(HttpStatus.NOT_FOUND, exception, MessageKeys.RESOURCE_NOT_FOUND);
    }

    @ExceptionHandler(IdentityProviderException.class)
    ResponseEntity<ApiError> identityProvider(IdentityProviderException exception) {
        return business(
                HttpStatus.SERVICE_UNAVAILABLE,
                exception,
                MessageKeys.IDENTITY_PROVIDER_UNAVAILABLE);
    }

    @ExceptionHandler(DuplicateResourceException.class)
    ResponseEntity<ApiError> conflict(DuplicateResourceException exception) {
        return business(HttpStatus.CONFLICT, exception, MessageKeys.RESOURCE_CONFLICT);
    }

    @ExceptionHandler(InsufficientStockException.class)
    ResponseEntity<ApiError> insufficientStock(InsufficientStockException exception) {
        return business(HttpStatus.CONFLICT, exception, MessageKeys.STOCK_INSUFFICIENT);
    }

    @ExceptionHandler(IllegalStateException.class)
    ResponseEntity<ApiError> invalidState(IllegalStateException exception) {
        return business(HttpStatus.CONFLICT, exception, MessageKeys.REQUEST_INVALID_STATE);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiError> validation(MethodArgumentNotValidException exception) {
        Map<String, String> errors = new LinkedHashMap<>();
        var fieldErrors = exception.getBindingResult().getFieldErrors();
        for (var error : fieldErrors) {
            errors.putIfAbsent(error.getField(), error.getDefaultMessage());
        }
        return build(HttpStatus.BAD_REQUEST, MessageKeys.VALIDATION_FAILED, errors);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<ApiError> dataIntegrity(DataIntegrityViolationException exception) {
        return build(HttpStatus.CONFLICT, MessageKeys.RESOURCE_CONFLICT, Map.of());
    }

    @ExceptionHandler(AccessDeniedException.class)
    ResponseEntity<ApiError> accessDenied(AccessDeniedException exception) {
        return build(HttpStatus.FORBIDDEN, MessageKeys.SECURITY_ACCESS_DENIED, Map.of());
    }

    @ExceptionHandler(AuthenticationException.class)
    ResponseEntity<ApiError> authenticationFailure(AuthenticationException exception) {
        if (exception instanceof AuthenticationServiceException) {
            log.error("Authentication failed due to a system error", exception);
            return build(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    MessageKeys.SECURITY_AUTHENTICATION_UNAVAILABLE,
                    Map.of());
        }
        return build(
                HttpStatus.UNAUTHORIZED, MessageKeys.SECURITY_AUTHENTICATION_REQUIRED, Map.of());
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ApiError> unexpectedFailure(Exception exception) {
        if (exception instanceof TypeMismatchException
                && !(exception instanceof ConversionNotSupportedException)) {
            return build(HttpStatus.BAD_REQUEST, MessageKeys.REQUEST_INVALID_PARAMETER, Map.of());
        }
        if (exception instanceof ErrorResponse error) {
            var status = error.getStatusCode();
            if (status.is4xxClientError()) {
                return ResponseEntity.status(status)
                        .headers(error.getHeaders())
                        .varyBy("Accept-Language")
                        .body(
                                ApiError.of(
                                        status,
                                        MessageKeys.REQUEST_REJECTED,
                                        messages.get(MessageKeys.REQUEST_REJECTED),
                                        Map.of()));
            }
        }
        log.error("Unexpected request processing failure", exception);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, MessageKeys.INTERNAL_ERROR, Map.of());
    }

    private ResponseEntity<ApiError> business(
            HttpStatus status, Exception exception, String fallback) {
        if (exception instanceof LocalizedError localized
                && localized.getMessage().matches("[a-z]+(?:[._][a-z]+)+")) {
            String key = localized.getMessage();
            return ResponseEntity.status(status)
                    .varyBy("Accept-Language")
                    .body(
                            ApiError.of(
                                    status,
                                    key,
                                    messages.get(key, localized.getArguments()),
                                    Map.of()));
        }
        return build(status, fallback, Map.of());
    }

    private ResponseEntity<ApiError> build(
            HttpStatus status, String message, Map<String, String> errors) {
        return ResponseEntity.status(status)
                .varyBy("Accept-Language")
                .body(ApiError.of(status, message, messages.get(message), errors));
    }
}
