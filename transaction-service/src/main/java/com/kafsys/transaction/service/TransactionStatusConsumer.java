package com.kafsys.transaction.service;

import com.kafsys.common.enums.AlertType;
import com.kafsys.common.enums.TransactionStatus;
import com.kafsys.common.event.AlertEvent;
import com.kafsys.common.event.TransactionEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class TransactionStatusConsumer {

    private static final Logger log = LoggerFactory.getLogger(TransactionStatusConsumer.class);

    private final TransactionService transactionService;
    private final KafkaTemplate<String, AlertEvent> alertTemplate;

    public TransactionStatusConsumer(TransactionService transactionService,
                                     KafkaTemplate<String, AlertEvent> alertTemplate) {
        this.transactionService = transactionService;
        this.alertTemplate = alertTemplate;
    }

    @KafkaListener(topics = "transactions", groupId = "transaction-status-updater",
                   containerFactory = "transactionEventListenerFactory")
    public void consumeFinalStatus(TransactionEvent event) {
        TransactionStatus status = event.getStatus();

        if (status == TransactionStatus.COMPLETED
                || status == TransactionStatus.ROLLED_BACK
                || status == TransactionStatus.FAILED) {

            transactionService.updateTransactionStatus(
                    event.getTransactionId(), status, event.getRejectionSource());

            publishAlert(event, status);
        }
    }

    private void publishAlert(TransactionEvent event, TransactionStatus status) {
        AlertType alertType = switch (status) {
            case COMPLETED -> AlertType.TRANSACTION_COMPLETED;
            case ROLLED_BACK -> AlertType.TRANSACTION_ROLLED_BACK;
            case FAILED -> AlertType.TRANSACTION_FAILED;
            default -> null;
        };

        if (alertType == null) return;

        String message = switch (alertType) {
            case TRANSACTION_COMPLETED ->
                String.format("Transaction %s completed successfully. Amount: %s %s",
                    event.getTransactionId(), event.getAmount(), event.getCurrency());
            case TRANSACTION_ROLLED_BACK ->
                String.format("Transaction %s was rolled back. Source: %s",
                    event.getTransactionId(), event.getRejectionSource());
            case TRANSACTION_FAILED ->
                String.format("Transaction %s failed. Reason: %s",
                    event.getTransactionId(), event.getRejectionSource());
            default -> "Transaction status update: " + status;
        };

        AlertEvent alert = new AlertEvent(
                UUID.randomUUID().toString(),
                event.getSourceAccountId(),
                event.getTransactionId(),
                alertType,
                message
        );

        alertTemplate.send("alerts", alert.getAccountId(), alert);
        log.info("Alert published: type={} txId={}", alertType, event.getTransactionId());
    }
}
