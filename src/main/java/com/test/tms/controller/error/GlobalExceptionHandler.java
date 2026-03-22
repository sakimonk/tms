package com.test.tms.controller.error;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleMethodArgumentNotValid(MethodArgumentNotValidException ex) {
        log.warn("Validation error", ex);

        List<ApiError.FieldViolation> violations = new ArrayList<>();
        if (ex.getBindingResult() != null) {
            ex.getBindingResult().getFieldErrors().forEach(fe -> violations.add(
                    new ApiError.FieldViolation(fe.getField(), fe.getDefaultMessage())
            ));
            ex.getBindingResult().getGlobalErrors().forEach(oe -> violations.add(
                    new ApiError.FieldViolation(oe.getObjectName(), oe.getDefaultMessage())
            ));
        }

        ApiError body = new ApiError(
                HttpStatus.BAD_REQUEST.value(),
                "Bad Request",
                "Request validation failed",
                Instant.now(),
                violations
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiError> handleConstraintViolation(ConstraintViolationException ex) {
        log.warn("Constraint violation error: {}", ex.getMessage());

        List<ApiError.FieldViolation> violations = new ArrayList<>();
        for (ConstraintViolation<?> v : ex.getConstraintViolations()) {
            String field = v.getPropertyPath() != null ? v.getPropertyPath().toString() : null;
            violations.add(new ApiError.FieldViolation(field, v.getMessage()));
        }

        ApiError body = new ApiError(
                HttpStatus.BAD_REQUEST.value(),
                "Bad Request",
                "Request validation failed",
                Instant.now(),
                violations
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    /**
     * Spring Framework 6.2+：控制器方法参数（含 {@code @RequestBody}）校验失败时可能抛出此异常，
     * 其为 {@link ResponseStatusException} 子类，需优先于 {@link #handleResponseStatusException} 处理。
     */
    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<ApiError> handleHandlerMethodValidation(HandlerMethodValidationException ex) {
        log.warn("Handler method validation error", ex);

        List<ApiError.FieldViolation> violations = new ArrayList<>();
        for (MessageSourceResolvable res : ex.getAllErrors()) {
            String field;
            String message;
            if (res instanceof FieldError fe) {
                field = fe.getField();
                message = fe.getDefaultMessage();
            } else if (res instanceof ObjectError oe) {
                field = oe.getObjectName();
                message = oe.getDefaultMessage();
            } else if (res instanceof DefaultMessageSourceResolvable dm) {
                field = "request";
                message = dm.getDefaultMessage();
            } else {
                field = "request";
                message = res.toString();
            }
            violations.add(new ApiError.FieldViolation(field, message));
        }

        ApiError body = new ApiError(
                HttpStatus.BAD_REQUEST.value(),
                "Bad Request",
                "Request validation failed",
                Instant.now(),
                violations
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ApiError> handleResponseStatusException(ResponseStatusException ex) {
        HttpStatus status = ex.getStatusCode() instanceof HttpStatus ? (HttpStatus) ex.getStatusCode() : HttpStatus.BAD_REQUEST;

        if (status.is5xxServerError()) {
            log.error("Request failed: status={}, reason={}", ex.getStatusCode().value(), ex.getReason(), ex);
        } else {
            log.warn("Request failed: status={}, reason={}", ex.getStatusCode().value(), ex.getReason());
        }

        ApiError body = new ApiError(
                status.value(),
                status.getReasonPhrase(),
                ex.getReason() != null ? ex.getReason() : ex.getMessage(),
                Instant.now(),
                null
        );
        return ResponseEntity.status(status).body(body);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiError> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        log.warn("Type mismatch error: {}", ex.getMessage());
        ApiError body = new ApiError(
                HttpStatus.BAD_REQUEST.value(),
                "Bad Request",
                "Request parameter type mismatch",
                Instant.now(),
                List.of(new ApiError.FieldViolation(ex.getName(), ex.getMessage()))
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleUnhandled(Exception ex) {
        log.error("Unhandled exception", ex);
        ApiError body = new ApiError(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "Internal Server Error",
                "Internal server error",
                Instant.now(),
                null
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }
}

