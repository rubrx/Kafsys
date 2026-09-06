package com.kafsys.transaction.service;

import com.kafsys.common.enums.TransactionStatus;
import com.kafsys.common.enums.TransactionType;
import com.kafsys.common.event.TransactionEvent;
import com.kafsys.common.exception.DuplicateTransactionException;
import com.kafsys.common.exception.ResourceNotFoundException;
import com.kafsys.transaction.dto.TransactionResponse;
import com.kafsys.transaction.dto.TransferRequest;
import com.kafsys.transaction.entity.Transaction;
import com.kafsys.transaction.repository.TransactionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock TransactionRepository repository;
    @SuppressWarnings("unchecked")
    @Mock KafkaTemplate<String, TransactionEvent> template;

    @InjectMocks TransactionService service;

    private TransferRequest transfer() {
        return new TransferRequest(
                "tx-key-1", "src-acct", "dst-acct",
                new BigDecimal("250.00"), "USD", "rent");
    }

    @Test
    void initiateTransfer_persistsAndPublishesEvent() {
        when(repository.existsById("tx-key-1")).thenReturn(false);
        when(repository.save(any(Transaction.class))).thenAnswer(inv -> inv.getArgument(0));

        TransactionResponse response = service.initiateTransfer(transfer());

        assertThat(response.id()).isEqualTo("tx-key-1");
        ArgumentCaptor<Transaction> saved = ArgumentCaptor.forClass(Transaction.class);
        verify(repository).save(saved.capture());
        assertThat(saved.getValue().getStatus()).isEqualTo(TransactionStatus.INITIATED);
        assertThat(saved.getValue().getType()).isEqualTo(TransactionType.FUND_TRANSFER);
        assertThat(saved.getValue().getAmount()).isEqualByComparingTo("250.00");

        ArgumentCaptor<TransactionEvent> event = ArgumentCaptor.forClass(TransactionEvent.class);
        verify(template).send(eq("transactions"), eq("tx-key-1"), event.capture());
        assertThat(event.getValue().getSourceAccountId()).isEqualTo("src-acct");
        assertThat(event.getValue().getDestinationAccountId()).isEqualTo("dst-acct");
        assertThat(event.getValue().getReferenceNote()).isEqualTo("rent");
        assertThat(event.getValue().getStatus()).isEqualTo(TransactionStatus.INITIATED);
    }

    @Test
    void initiateTransfer_duplicateKey_throws_andSkipsPublish() {
        when(repository.existsById("tx-key-1")).thenReturn(true);

        assertThatThrownBy(() -> service.initiateTransfer(transfer()))
                .isInstanceOf(DuplicateTransactionException.class)
                .hasMessageContaining("tx-key-1");
        verify(repository, never()).save(any());
        verifyNoInteractions(template);
    }

    @Test
    void getById_missing_throws() {
        when(repository.findById("nope")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.getById("nope"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void updateTransactionStatus_terminalStatus_setsCompletedAt() {
        Transaction tx = new Transaction();
        tx.setId("tx-1");
        tx.setStatus(TransactionStatus.PAYMENT_PROCESSED);
        when(repository.findById("tx-1")).thenReturn(Optional.of(tx));

        service.updateTransactionStatus("tx-1", TransactionStatus.COMPLETED, null);

        assertThat(tx.getStatus()).isEqualTo(TransactionStatus.COMPLETED);
        assertThat(tx.getCompletedAt()).isNotNull();
        verify(repository).save(tx);
    }

    @Test
    void updateTransactionStatus_intermediateStatus_leavesCompletedAtNull() {
        Transaction tx = new Transaction();
        tx.setId("tx-1");
        tx.setStatus(TransactionStatus.INITIATED);
        when(repository.findById("tx-1")).thenReturn(Optional.of(tx));

        service.updateTransactionStatus("tx-1", TransactionStatus.ACCOUNT_VALIDATED, null);

        assertThat(tx.getStatus()).isEqualTo(TransactionStatus.ACCOUNT_VALIDATED);
        assertThat(tx.getCompletedAt()).isNull();
    }

    @Test
    void updateTransactionStatus_rolledBack_recordsRejectionSource() {
        Transaction tx = new Transaction();
        tx.setId("tx-1");
        tx.setStatus(TransactionStatus.ACCOUNT_VALIDATED);
        when(repository.findById("tx-1")).thenReturn(Optional.of(tx));

        service.updateTransactionStatus("tx-1", TransactionStatus.ROLLED_BACK, "payment-gateway");

        assertThat(tx.getStatus()).isEqualTo(TransactionStatus.ROLLED_BACK);
        assertThat(tx.getRejectionSource()).isEqualTo("payment-gateway");
        assertThat(tx.getCompletedAt()).isNotNull();
    }

    @Test
    void updateTransactionStatus_missingTx_isSilentNoOp() {
        when(repository.findById("ghost")).thenReturn(Optional.empty());
        service.updateTransactionStatus("ghost", TransactionStatus.COMPLETED, null);
        verify(repository, never()).save(any());
    }
}
