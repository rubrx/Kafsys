package com.kafsys.payment.service;

import com.kafsys.common.enums.TransactionStatus;
import com.kafsys.common.event.TransactionEvent;
import com.kafsys.payment.entity.Payment;
import com.kafsys.payment.entity.PaymentStatus;
import com.kafsys.payment.repository.PaymentRepository;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);
    private static final String SOURCE = "PAYMENT";

    private final PaymentRepository paymentRepository;
    private final KafkaTemplate<String, TransactionEvent> kafkaTemplate;

    public PaymentService(PaymentRepository paymentRepository,
                          KafkaTemplate<String, TransactionEvent> kafkaTemplate) {
        this.paymentRepository = paymentRepository;
        this.kafkaTemplate = kafkaTemplate;
    }

    @KafkaListener(topics = "transactions", groupId = "payment-processor",
                   containerFactory = "transactionEventListenerFactory")
    @Transactional
    public void handleTransactionInitiated(TransactionEvent event) {
        if (event.getStatus() != TransactionStatus.INITIATED) {
            return;
        }
        if (paymentRepository.existsByTransactionId(event.getTransactionId())) {
            log.warn("Duplicate payment processing attempt for txId={}", event.getTransactionId());
            return;
        }

        log.info("Processing payment: txId={} amount={} {}", event.getTransactionId(),
                event.getAmount(), event.getCurrency());

        Payment payment = new Payment();
        payment.setTransactionId(event.getTransactionId());
        payment.setSourceAccountId(event.getSourceAccountId());
        payment.setDestinationAccountId(event.getDestinationAccountId());
        payment.setAmount(event.getAmount());
        payment.setCurrency(event.getCurrency());

        try {
            String gatewayRef = processWithExternalGateway(event);
            payment.setStatus(PaymentStatus.PROCESSED);
            payment.setGatewayReference(gatewayRef);
            event.setStatus(TransactionStatus.PAYMENT_PROCESSED);
        } catch (Exception e) {
            payment.setStatus(PaymentStatus.FAILED);
            payment.setFailureReason(e.getMessage());
            event.setStatus(TransactionStatus.PAYMENT_FAILED);
            event.setRejectionSource(SOURCE + ": " + e.getMessage());
            log.error("Payment failed: txId={} reason={}", event.getTransactionId(), e.getMessage());
        }

        paymentRepository.save(payment);
        kafkaTemplate.send("payment-transactions", event.getTransactionId(), event);
    }

    @KafkaListener(topics = "transactions", groupId = "payment-finalizer",
                   containerFactory = "transactionEventListenerFactory")
    @Transactional
    public void handleTransactionFinal(TransactionEvent event) {
        if (event.getStatus() == TransactionStatus.COMPLETED) {
            paymentRepository.findByTransactionId(event.getTransactionId()).ifPresent(payment -> {
                payment.setStatus(PaymentStatus.SETTLED);
                payment.setSettledAt(LocalDateTime.now());
                paymentRepository.save(payment);
                log.info("Payment settled: txId={}", event.getTransactionId());
            });
        } else if (event.getStatus() == TransactionStatus.ROLLED_BACK
                && SOURCE.equals(event.getRejectionSource())) {
            paymentRepository.findByTransactionId(event.getTransactionId()).ifPresent(payment -> {
                payment.setStatus(PaymentStatus.REVERSED);
                paymentRepository.save(payment);
                log.info("Payment reversed: txId={}", event.getTransactionId());
            });
        }
    }

    @CircuitBreaker(name = "external-gateway", fallbackMethod = "gatewayFallback")
    private String processWithExternalGateway(TransactionEvent event) {
        // Simulates external payment gateway call.
        // In production: integrate with Stripe, Adyen, or your bank's payment API.
        return "GW-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    private String gatewayFallback(TransactionEvent event, Exception ex) {
        throw new RuntimeException("Payment gateway unavailable: " + ex.getMessage());
    }
}
