package com.kafsys.transaction.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record TransferRequest(
        @NotBlank(message = "Idempotency key is required")
        @Size(max = 36, message = "Idempotency key must not exceed 36 characters")
        String idempotencyKey,

        @NotBlank(message = "Source account ID is required")
        String sourceAccountId,

        @NotBlank(message = "Destination account ID is required")
        String destinationAccountId,

        @NotNull(message = "Amount is required")
        @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
        BigDecimal amount,

        @NotBlank(message = "Currency is required")
        @Size(min = 3, max = 3, message = "Currency must be a 3-letter ISO code")
        String currency,

        @Size(max = 255)
        String referenceNote
) {
}
