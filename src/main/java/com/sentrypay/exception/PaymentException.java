package com.sentrypay.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class PaymentException extends RuntimeException {
    private final HttpStatus status;

    public PaymentException(String message, HttpStatus status) {
        super(message);
        this.status = status;
    }
}
