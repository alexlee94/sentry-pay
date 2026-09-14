package com.sentrypay.controller;

import com.sentrypay.dto.WebhookDTOs.RefundWebhookPayload;
import com.sentrypay.service.RefundEventService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/webhooks")
@RequiredArgsConstructor
public class WebhookController {

    private final RefundEventService refundEventService;

    @PostMapping("/refund")
    public ResponseEntity<Void> handleRefundWebhook(@RequestBody RefundWebhookPayload payload) {
        refundEventService.handleRefundWebhook(payload);
        return ResponseEntity.ok().build();
    }
}
