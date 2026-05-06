package com.kafsys.transaction.service;

import com.kafsys.common.dto.PagedResponse;
import com.kafsys.common.enums.TransactionStatus;
import com.kafsys.common.enums.TransactionType;
import com.kafsys.common.event.TransactionEvent;
import com.kafsys.common.exception.DuplicateTransactionException;
import com.kafsys.common.exception.ResourceNotFoundException;
import com.kafsys.transaction.dto.TransactionResponse;
import com.kafsys.transaction.dto.TransferRequest;
import com.kafsys.transaction.entity.Transaction;
import com.kafsys.transaction.repository.TransactionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class TransactionService {

    private static final Logger log = LoggerFactory.getLogger(TransactionService.class);

    private final TransactionRepository transactionRepository;
    private final KafkaTemplate<String, TransactionEvent> kafkaTemplate;

    public TransactionService(TransactionRepository transactionRepository,
                              KafkaTemplate<String, TransactionEvent> kafkaTemplate) {
        this.transactionRepository = transactionRepository;
        this.kafkaTemplate = kafkaTemplate;
    }

    @Transactional
    public TransactionResponse initiateTransfer(TransferRequest request) {
        if (transactionRepository.existsById(request.idempotencyKey())) {
            throw new DuplicateTransactionException(request.idempotencyKey());
        }

        Transaction tx = new Transaction();
        tx.setId(request.idempotencyKey());
        tx.setSourceAccountId(request.sourceAccountId());
        tx.setDestinationAccountId(request.destinationAccountId());
        tx.setAmount(request.amount());
        tx.setCurrency(request.currency());
        tx.setType(TransactionType.FUND_TRANSFER);
        tx.setStatus(TransactionStatus.INITIATED);
        tx.setReferenceNote(request.referenceNote());
        transactionRepository.save(tx);

        TransactionEvent event = new TransactionEvent(
                tx.getId(), tx.getSourceAccountId(), tx.getDestinationAccountId(),
                tx.getAmount(), tx.getCurrency(), tx.getType()
        );
        event.setReferenceNote(tx.getReferenceNote());

        kafkaTemplate.send("transactions", tx.getId(), event);
        log.info("Transaction initiated: id={} amount={} {}", tx.getId(), tx.getAmount(), tx.getCurrency());

        return TransactionResponse.from(tx);
    }

    @Cacheable(value = "transactions", key = "#id")
    @Transactional(readOnly = true)
    public TransactionResponse getById(String id) {
        return transactionRepository.findById(id)
                .map(TransactionResponse::from)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction", id));
    }

    @Transactional(readOnly = true)
    public PagedResponse<TransactionResponse> getTransactions(
            String sourceAccountId, TransactionStatus status, TransactionType type,
            LocalDateTime fromDate, LocalDateTime toDate, int page, int size) {

        Pageable pageable = PageRequest.of(page, size);
        Page<Transaction> results = transactionRepository.findWithFilters(
                sourceAccountId, status, type, fromDate, toDate, pageable);

        return PagedResponse.of(
                results.getContent().stream().map(TransactionResponse::from).toList(),
                page, size, results.getTotalElements()
        );
    }

    @Transactional
    @CacheEvict(value = "transactions", key = "#transactionId")
    public void updateTransactionStatus(String transactionId, TransactionStatus status, String rejectionSource) {
        transactionRepository.findById(transactionId).ifPresent(tx -> {
            tx.setStatus(status);
            tx.setRejectionSource(rejectionSource);
            if (status == TransactionStatus.COMPLETED || status == TransactionStatus.ROLLED_BACK
                    || status == TransactionStatus.FAILED) {
                tx.setCompletedAt(LocalDateTime.now());
            }
            transactionRepository.save(tx);
            log.info("Transaction status updated: id={} newStatus={}", transactionId, status);
        });
    }
}
