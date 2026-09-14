package com.sentrypay.repository;

import com.sentrypay.entity.RefundEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RefundEventRepository extends JpaRepository<RefundEvent, Long> {
    Optional<RefundEvent> findByStripeEventId(String stripeEventId);
    List<RefundEvent> findByStatusAndRetryCountLessThan(RefundEvent.ProcessingStatus status, int maxRetries);
}
