package com.kafsys.payment.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "payments", indexes = {
        @Index(name = "idx_payments_transaction_id", columnList = "transaction_id"),
        @Index(name = "idx_payments_status", columnList = "status"),
        @Index(name = "idx_payments_processed_at", columnList = "processed_at")
})
public class Payment {

    @Id
    @Column(name = "id", nullable = false, updatable = false, length = 36)
    private String id;

    @Column(name = "transaction_id", nullable = false, unique = true, length = 36)
    private String transactionId;

    @Column(name = "source_account_id", nullable = false, length = 36)
    private String sourceAccountId;

    @Column(name = "destination_account_id", nullable = false, length = 36)
    private String destinationAccountId;

    @Column(name = "amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private PaymentStatus status;

    @Column(name = "gateway_reference", length = 100)
    private String gatewayReference;

    @Column(name = "failure_reason", length = 255)
    private String failureReason;

    @Column(name = "processed_at", nullable = false, updatable = false)
    private LocalDateTime processedAt;

    @Column(name = "settled_at")
    private LocalDateTime settledAt;

    @PrePersist
    protected void onCreate() {
        if (id == null) id = UUID.randomUUID().toString();
        processedAt = LocalDateTime.now();
    }

    public Payment() {
    }

    public String getId() { return id; }
    public String getTransactionId() { return transactionId; }
    public void setTransactionId(String transactionId) { this.transactionId = transactionId; }
    public String getSourceAccountId() { return sourceAccountId; }
    public void setSourceAccountId(String sourceAccountId) { this.sourceAccountId = sourceAccountId; }
    public String getDestinationAccountId() { return destinationAccountId; }
    public void setDestinationAccountId(String destinationAccountId) { this.destinationAccountId = destinationAccountId; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    public PaymentStatus getStatus() { return status; }
    public void setStatus(PaymentStatus status) { this.status = status; }
    public String getGatewayReference() { return gatewayReference; }
    public void setGatewayReference(String gatewayReference) { this.gatewayReference = gatewayReference; }
    public String getFailureReason() { return failureReason; }
    public void setFailureReason(String failureReason) { this.failureReason = failureReason; }
    public LocalDateTime getProcessedAt() { return processedAt; }
    public LocalDateTime getSettledAt() { return settledAt; }
    public void setSettledAt(LocalDateTime settledAt) { this.settledAt = settledAt; }
}
