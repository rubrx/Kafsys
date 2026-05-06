package com.kafsys.alert.dto;

import com.kafsys.alert.entity.Alert;
import com.kafsys.common.enums.AlertType;

import java.time.LocalDateTime;

public record AlertResponse(
        String id,
        String accountId,
        String transactionId,
        AlertType type,
        String message,
        boolean read,
        LocalDateTime triggeredAt,
        LocalDateTime readAt
) {
    public static AlertResponse from(Alert alert) {
        return new AlertResponse(
                alert.getId(),
                alert.getAccountId(),
                alert.getTransactionId(),
                alert.getType(),
                alert.getMessage(),
                alert.isRead(),
                alert.getTriggeredAt(),
                alert.getReadAt()
        );
    }
}
