package com.kafsys.account.service;

import com.kafsys.common.enums.TransactionStatus;
import com.kafsys.common.event.TransactionEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class TransactionEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(TransactionEventConsumer.class);
    private static final String SOURCE = "ACCOUNT";

    private final AccountService accountService;
    private final KafkaTemplate<String, TransactionEvent> kafkaTemplate;

    public TransactionEventConsumer(AccountService accountService,
                                    KafkaTemplate<String, TransactionEvent> kafkaTemplate) {
        this.accountService = accountService;
        this.kafkaTemplate = kafkaTemplate;
    }

    @KafkaListener(topics = "transactions", groupId = "account-validator",
                   containerFactory = "transactionEventListenerFactory")
    public void handleTransactionInitiated(TransactionEvent event) {
        if (event.getStatus() != TransactionStatus.INITIATED) {
            return;
        }

        log.info("Validating account balance: txId={} sourceAccount={} amount={}",
                event.getTransactionId(), event.getSourceAccountId(), event.getAmount());

        try {
            accountService.reserveBalance(event.getSourceAccountId(), event.getAmount());
            event.setStatus(TransactionStatus.ACCOUNT_VALIDATED);
            log.info("Balance reserved: txId={} account={}", event.getTransactionId(), event.getSourceAccountId());
        } catch (Exception e) {
            event.setStatus(TransactionStatus.ACCOUNT_REJECTED);
            event.setRejectionSource(SOURCE + ": " + e.getMessage());
            log.warn("Balance reservation failed: txId={} reason={}", event.getTransactionId(), e.getMessage());
        }

        kafkaTemplate.send("account-transactions", event.getTransactionId(), event);
    }

    @KafkaListener(topics = "transactions", groupId = "account-finalizer",
                   containerFactory = "transactionEventListenerFactory")
    public void handleTransactionFinal(TransactionEvent event) {
        switch (event.getStatus()) {
            case COMPLETED -> {
                accountService.confirmDebit(event.getSourceAccountId(), event.getAmount());
                accountService.creditAccount(event.getDestinationAccountId(), event.getAmount());
                log.info("Transaction finalized (debit+credit): txId={}", event.getTransactionId());
            }
            case ROLLED_BACK -> {
                if (SOURCE.equals(event.getRejectionSource()) || event.getRejectionSource() == null
                        || !event.getRejectionSource().startsWith(SOURCE)) {
                    return;
                }
                accountService.releaseReservation(event.getSourceAccountId(), event.getAmount());
                log.info("Reservation released (rollback): txId={}", event.getTransactionId());
            }
            default -> { /* no-op for other statuses */ }
        }
    }
}
