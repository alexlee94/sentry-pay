package com.sentrypay.controller;

import com.sentrypay.dto.PaymentDTOs.CreatePaymentRequest;
import com.sentrypay.dto.PaymentDTOs.PaymentResponse;
import com.sentrypay.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping
    public ResponseEntity<PaymentResponse> createPayment(
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody CreatePaymentRequest request) {

        PaymentResponse response = paymentService.createPayment(idempotencyKey, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
