package com.kafsys.payment.repository;

import com.kafsys.payment.entity.Payment;
import com.kafsys.payment.entity.PaymentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, String> {

    Optional<Payment> findByTransactionId(String transactionId);

    Page<Payment> findBySourceAccountId(String sourceAccountId, Pageable pageable);

    Page<Payment> findByStatus(PaymentStatus status, Pageable pageable);

    boolean existsByTransactionId(String transactionId);
}
