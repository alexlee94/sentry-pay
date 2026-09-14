package com.sentrypay.service;

import com.sentrypay.dto.WebhookDTOs.RefundWebhookPayload;
import com.sentrypay.entity.Payment;
import com.sentrypay.entity.RefundEvent;
import com.sentrypay.repository.PaymentRepository;
import com.sentrypay.repository.RefundEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class RefundEventService {

    private static final int MAX_RETRIES = 5;

    private final RefundEventRepository refundEventRepository;
    private final PaymentRepository paymentRepository;

    // ── Receive webhook ───────────────────────────────────────────────────────

    @Transactional
    public void handleRefundWebhook(RefundWebhookPayload payload) {
        // Avoid processing the same Stripe event twice
        if (refundEventRepository.findByStripeEventId(payload.getStripeEventId()).isPresent()) {
            log.info("Duplicate webhook event {}, ignoring", payload.getStripeEventId());
            return;
        }

        RefundEvent event = RefundEvent.builder()
                .stripeChargeId(payload.getStripeChargeId())
                .stripeEventId(payload.getStripeEventId())
                .payload(payload.getRawPayload())
                .status(RefundEvent.ProcessingStatus.PENDING)
                .retryCount(0)
                .createdAt(Instant.now())
                .build();

        refundEventRepository.save(event);

        // Try processing immediately — if the parent charge doesn't exist yet,
        // it stays PENDING and gets picked up by the retry job below
        tryProcessEvent(event);
    }

    // ── Retry queue — reprocesses out-of-order events ────────────────────────

    @Scheduled(fixedRate = 10000) // every 10 seconds
    @Transactional
    public void reprocessPendingEvents() {
        List<RefundEvent> pending = refundEventRepository
                .findByStatusAndRetryCountLessThan(RefundEvent.ProcessingStatus.PENDING, MAX_RETRIES);

        for (RefundEvent event : pending) {
            tryProcessEvent(event);
        }
    }

    // ── Core processing logic ─────────────────────────────────────────────────

    private void tryProcessEvent(RefundEvent event) {
        Optional<Payment> payment = paymentRepository.findByStripePaymentIntentId(event.getStripeChargeId());

        if (payment.isEmpty()) {
            // Parent charge doesn't exist yet — this is the out-of-order case
            event.setRetryCount(event.getRetryCount() + 1);
            refundEventRepository.save(event);
            log.info("Parent charge {} not found yet for event {}, retry {}/{}",
                    event.getStripeChargeId(), event.getStripeEventId(), event.getRetryCount(), MAX_RETRIES);
            return;
        }

        // Parent charge exists — mark the payment refunded and complete the event
        Payment p = payment.get();
        p.setStatus(Payment.PaymentStatus.REFUNDED);
        p.setUpdatedAt(Instant.now());
        paymentRepository.save(p);

        event.setStatus(RefundEvent.ProcessingStatus.PROCESSED);
        event.setProcessedAt(Instant.now());
        refundEventRepository.save(event);

        log.info("Successfully processed refund event {} for charge {}",
                event.getStripeEventId(), event.getStripeChargeId());
    }
}
