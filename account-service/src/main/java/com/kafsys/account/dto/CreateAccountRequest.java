package com.kafsys.account.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record CreateAccountRequest(
        @NotBlank(message = "Owner ID is required")
        String ownerId,

        @NotBlank(message = "Owner name is required")
        @Size(max = 100)
        String ownerName,

        @DecimalMin(value = "0.0", message = "Initial deposit cannot be negative")
        BigDecimal initialDeposit,

        @NotBlank(message = "Currency is required")
        @Size(min = 3, max = 3, message = "Currency must be a 3-letter ISO code")
        String currency
) {
}
