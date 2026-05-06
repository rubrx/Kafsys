package com.kafsys.payment.dto;

import com.kafsys.payment.entity.Payment;
import com.kafsys.payment.entity.PaymentStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PaymentResponse(
        String id,
        String transactionId,
        String sourceAccountId,
        String destinationAccountId,
        BigDecimal amount,
        String currency,
        PaymentStatus status,
        String gatewayReference,
        String failureReason,
        LocalDateTime processedAt,
        LocalDateTime settledAt
) {
    public static PaymentResponse from(Payment payment) {
        return new PaymentResponse(
                payment.getId(),
                payment.getTransactionId(),
                payment.getSourceAccountId(),
                payment.getDestinationAccountId(),
                payment.getAmount(),
                payment.getCurrency(),
                payment.getStatus(),
                payment.getGatewayReference(),
                payment.getFailureReason(),
                payment.getProcessedAt(),
                payment.getSettledAt()
        );
    }
}
