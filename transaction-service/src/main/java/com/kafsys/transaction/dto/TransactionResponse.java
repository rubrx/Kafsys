package com.kafsys.transaction.dto;

import com.kafsys.common.enums.TransactionStatus;
import com.kafsys.common.enums.TransactionType;
import com.kafsys.transaction.entity.Transaction;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TransactionResponse(
        String id,
        String sourceAccountId,
        String destinationAccountId,
        BigDecimal amount,
        String currency,
        TransactionType type,
        TransactionStatus status,
        String referenceNote,
        String rejectionSource,
        LocalDateTime initiatedAt,
        LocalDateTime completedAt
) {
    public static TransactionResponse from(Transaction tx) {
        return new TransactionResponse(
                tx.getId(),
                tx.getSourceAccountId(),
                tx.getDestinationAccountId(),
                tx.getAmount(),
                tx.getCurrency(),
                tx.getType(),
                tx.getStatus(),
                tx.getReferenceNote(),
                tx.getRejectionSource(),
                tx.getInitiatedAt(),
                tx.getCompletedAt()
        );
    }
}
