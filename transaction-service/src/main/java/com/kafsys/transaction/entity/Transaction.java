package com.kafsys.transaction.entity;

import com.kafsys.common.enums.TransactionStatus;
import com.kafsys.common.enums.TransactionType;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "transactions", indexes = {
        @Index(name = "idx_tx_source_account", columnList = "source_account_id"),
        @Index(name = "idx_tx_dest_account", columnList = "destination_account_id"),
        @Index(name = "idx_tx_status", columnList = "status"),
        @Index(name = "idx_tx_initiated_at", columnList = "initiated_at")
})
public class Transaction {

    @Id
    @Column(name = "id", nullable = false, updatable = false, length = 36)
    private String id;

    @Column(name = "source_account_id", nullable = false, length = 36)
    private String sourceAccountId;

    @Column(name = "destination_account_id", nullable = false, length = 36)
    private String destinationAccountId;

    @Column(name = "amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 20)
    private TransactionType type;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private TransactionStatus status;

    @Column(name = "reference_note", length = 255)
    private String referenceNote;

    @Column(name = "rejection_source", length = 50)
    private String rejectionSource;

    @Column(name = "initiated_at", nullable = false, updatable = false)
    private LocalDateTime initiatedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Version
    @Column(name = "version")
    private Long version;

    @PrePersist
    protected void onCreate() {
        initiatedAt = LocalDateTime.now();
    }

    public Transaction() {
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getSourceAccountId() { return sourceAccountId; }
    public void setSourceAccountId(String sourceAccountId) { this.sourceAccountId = sourceAccountId; }
    public String getDestinationAccountId() { return destinationAccountId; }
    public void setDestinationAccountId(String destinationAccountId) { this.destinationAccountId = destinationAccountId; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    public TransactionType getType() { return type; }
    public void setType(TransactionType type) { this.type = type; }
    public TransactionStatus getStatus() { return status; }
    public void setStatus(TransactionStatus status) { this.status = status; }
    public String getReferenceNote() { return referenceNote; }
    public void setReferenceNote(String referenceNote) { this.referenceNote = referenceNote; }
    public String getRejectionSource() { return rejectionSource; }
    public void setRejectionSource(String rejectionSource) { this.rejectionSource = rejectionSource; }
    public LocalDateTime getInitiatedAt() { return initiatedAt; }
    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }
    public Long getVersion() { return version; }
}
