package com.sentrypay.dto;

import lombok.Data;

public class WebhookDTOs {

    @Data
    public static class RefundWebhookPayload {
        private String stripeEventId;
        private String stripeChargeId;
        private String rawPayload;
    }
}
