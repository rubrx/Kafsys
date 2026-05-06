package com.kafsys.transaction.service;

import com.kafsys.common.enums.TransactionStatus;
import com.kafsys.common.event.TransactionEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class TransactionSagaService {

    private static final Logger log = LoggerFactory.getLogger(TransactionSagaService.class);

    /**
     * Resolves the SAGA outcome by joining account validation and payment processing results.
     * Both services must accept for the transaction to COMPLETE.
     * Any rejection triggers a ROLLBACK with the originating source recorded.
     */
    public TransactionEvent resolveSaga(TransactionEvent accountEvent, TransactionEvent paymentEvent) {
        TransactionEvent resolved = copyBase(accountEvent);

        boolean accountAccepted = accountEvent.getStatus() == TransactionStatus.ACCOUNT_VALIDATED;
        boolean paymentAccepted = paymentEvent.getStatus() == TransactionStatus.PAYMENT_PROCESSED;

        if (accountAccepted && paymentAccepted) {
            resolved.setStatus(TransactionStatus.COMPLETED);
            resolved.setRejectionSource(null);
        } else if (!accountAccepted && !paymentAccepted) {
            resolved.setStatus(TransactionStatus.FAILED);
            resolved.setRejectionSource("ACCOUNT,PAYMENT");
        } else if (!accountAccepted) {
            resolved.setStatus(TransactionStatus.ROLLED_BACK);
            resolved.setRejectionSource("ACCOUNT");
        } else {
            resolved.setStatus(TransactionStatus.ROLLED_BACK);
            resolved.setRejectionSource("PAYMENT");
        }

        log.info("SAGA resolved: txId={} accountStatus={} paymentStatus={} => finalStatus={}",
                resolved.getTransactionId(),
                accountEvent.getStatus(),
                paymentEvent.getStatus(),
                resolved.getStatus());

        return resolved;
    }

    private TransactionEvent copyBase(TransactionEvent source) {
        TransactionEvent copy = new TransactionEvent();
        copy.setTransactionId(source.getTransactionId());
        copy.setSourceAccountId(source.getSourceAccountId());
        copy.setDestinationAccountId(source.getDestinationAccountId());
        copy.setAmount(source.getAmount());
        copy.setCurrency(source.getCurrency());
        copy.setType(source.getType());
        copy.setReferenceNote(source.getReferenceNote());
        copy.setInitiatedAt(source.getInitiatedAt());
        return copy;
    }
}
