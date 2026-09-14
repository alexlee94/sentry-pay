package com.sentrypay.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;

public class PaymentDTOs {

    @Data
    public static class CreatePaymentRequest {
        @NotNull
        @DecimalMin(value = "0.50")
        private BigDecimal amount;

        @NotBlank
        @Size(min = 3, max = 3)
        private String currency;

        @Email
        private String customerEmail;
    }

    @Data
    public static class PaymentResponse {
        private Long id;
        private String stripePaymentIntentId;
        private BigDecimal amount;
        private String currency;
        private String status;
        private String clientSecret;
    }
}
