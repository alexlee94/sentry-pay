package com.sentrypay.exception;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(PaymentException.class)
    public ResponseEntity<ErrorResponse> handlePayment(PaymentException ex) {
        return ResponseEntity.status(ex.getStatus())
                .body(new ErrorResponse(ex.getStatus().value(), ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ValidationErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> errors = ex.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.toMap(
                        FieldError::getField,
                        fe -> fe.getDefaultMessage() != null ? fe.getDefaultMessage() : "Invalid value",
                        (a, b) -> a
                ));
        return ResponseEntity.badRequest()
                .body(new ValidationErrorResponse(400, "Validation failed", errors));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(Exception ex) {
        return ResponseEntity.status(500)
                .body(new ErrorResponse(500, "An unexpected error occurred"));
    }

    public record ErrorResponse(int status, String message, Instant timestamp) {
        public ErrorResponse(int status, String message) {
            this(status, message, Instant.now());
        }
    }

    public record ValidationErrorResponse(int status, String message,
                                          Map<String, String> errors, Instant timestamp) {
        public ValidationErrorResponse(int status, String message, Map<String, String> errors) {
            this(status, message, errors, Instant.now());
        }
    }
}
