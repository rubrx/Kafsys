package com.kafsys.common.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.kafsys.common.enums.AlertType;

import java.time.LocalDateTime;

@JsonIgnoreProperties(ignoreUnknown = true)
public class AlertEvent {

    private String alertId;
    private String accountId;
    private String transactionId;
    private AlertType type;
    private String message;
    private LocalDateTime triggeredAt;

    public AlertEvent() {
    }

    public AlertEvent(String alertId, String accountId, String transactionId,
                      AlertType type, String message) {
        this.alertId = alertId;
        this.accountId = accountId;
        this.transactionId = transactionId;
        this.type = type;
        this.message = message;
        this.triggeredAt = LocalDateTime.now();
    }

    public String getAlertId() { return alertId; }
    public void setAlertId(String alertId) { this.alertId = alertId; }

    public String getAccountId() { return accountId; }
    public void setAccountId(String accountId) { this.accountId = accountId; }

    public String getTransactionId() { return transactionId; }
    public void setTransactionId(String transactionId) { this.transactionId = transactionId; }

    public AlertType getType() { return type; }
    public void setType(AlertType type) { this.type = type; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public LocalDateTime getTriggeredAt() { return triggeredAt; }
    public void setTriggeredAt(LocalDateTime triggeredAt) { this.triggeredAt = triggeredAt; }
}
