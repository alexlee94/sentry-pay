package com.sentrypay.service;

import com.sentrypay.dto.PaymentDTOs.CreatePaymentRequest;
import com.sentrypay.dto.PaymentDTOs.PaymentResponse;
import com.sentrypay.entity.Payment;
import com.sentrypay.exception.PaymentException;
import com.sentrypay.repository.PaymentRepository;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.param.PaymentIntentCreateParams;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private static final Duration IDEMPOTENCY_KEY_TTL = Duration.ofHours(24);
    private static final String IDEMPOTENCY_KEY_PREFIX = "idempotency:";

    private final PaymentRepository paymentRepository;
    private final RedisTemplate<String, String> redisTemplate;

    // ── Create Payment (idempotent) ──────────────────────────────────────────

    @Transactional
    public PaymentResponse createPayment(String idempotencyKey, CreatePaymentRequest request) {
        // Check Redis first — fast path for retries within the TTL window
        String cachedPaymentId = redisTemplate.opsForValue()
                .get(IDEMPOTENCY_KEY_PREFIX + idempotencyKey);

        if (cachedPaymentId != null) {
            log.info("Idempotency key {} found in Redis, returning cached result", idempotencyKey);
            Payment existing = paymentRepository.findById(Long.parseLong(cachedPaymentId))
                    .orElseThrow(() -> new PaymentException("Payment not found", HttpStatus.NOT_FOUND));
            return toResponse(existing, null);
        }

        // Fallback check against the database in case Redis was flushed/restarted
        var existingPayment = paymentRepository.findByIdempotencyKey(idempotencyKey);
        if (existingPayment.isPresent()) {
            log.info("Idempotency key {} found in database, returning cached result", idempotencyKey);
            cacheIdempotencyKey(idempotencyKey, existingPayment.get().getId());
            return toResponse(existingPayment.get(), null);
        }

        // No existing payment — process a new one
        try {
            PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                    .setAmount(request.getAmount().multiply(java.math.BigDecimal.valueOf(100)).longValue())
                    .setCurrency(request.getCurrency().toLowerCase())
                    .build();

            PaymentIntent intent = PaymentIntent.create(params);

            Payment payment = Payment.builder()
                    .idempotencyKey(idempotencyKey)
                    .stripePaymentIntentId(intent.getId())
                    .amount(request.getAmount())
                    .currency(request.getCurrency())
                    .status(Payment.PaymentStatus.PENDING)
                    .customerEmail(request.getCustomerEmail())
                    .createdAt(Instant.now())
                    .build();

            payment = paymentRepository.save(payment);
            cacheIdempotencyKey(idempotencyKey, payment.getId());

            return toResponse(payment, intent.getClientSecret());

        } catch (StripeException e) {
            log.error("Stripe error while creating payment intent", e);
            throw new PaymentException("Payment processing failed: " + e.getMessage(), HttpStatus.BAD_GATEWAY);
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void cacheIdempotencyKey(String idempotencyKey, Long paymentId) {
        redisTemplate.opsForValue().set(
                IDEMPOTENCY_KEY_PREFIX + idempotencyKey,
                paymentId.toString(),
                IDEMPOTENCY_KEY_TTL
        );
    }

    private PaymentResponse toResponse(Payment payment, String clientSecret) {
        PaymentResponse response = new PaymentResponse();
        response.setId(payment.getId());
        response.setStripePaymentIntentId(payment.getStripePaymentIntentId());
        response.setAmount(payment.getAmount());
        response.setCurrency(payment.getCurrency());
        response.setStatus(payment.getStatus().name());
        response.setClientSecret(clientSecret);
        return response;
    }
}
