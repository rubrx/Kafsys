package com.kafsys.transaction.repository;

import com.kafsys.common.enums.TransactionStatus;
import com.kafsys.common.enums.TransactionType;
import com.kafsys.transaction.entity.Transaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, String> {

    Page<Transaction> findBySourceAccountId(String sourceAccountId, Pageable pageable);

    Page<Transaction> findByDestinationAccountId(String destinationAccountId, Pageable pageable);

    Page<Transaction> findByStatus(TransactionStatus status, Pageable pageable);

    @Query("""
            SELECT t FROM Transaction t
            WHERE (:sourceAccountId IS NULL OR t.sourceAccountId = :sourceAccountId)
              AND (:status IS NULL OR t.status = :status)
              AND (:type IS NULL OR t.type = :type)
              AND (:fromDate IS NULL OR t.initiatedAt >= :fromDate)
              AND (:toDate IS NULL OR t.initiatedAt <= :toDate)
            ORDER BY t.initiatedAt DESC
            """)
    Page<Transaction> findWithFilters(
            @Param("sourceAccountId") String sourceAccountId,
            @Param("status") TransactionStatus status,
            @Param("type") TransactionType type,
            @Param("fromDate") LocalDateTime fromDate,
            @Param("toDate") LocalDateTime toDate,
            Pageable pageable
    );

    boolean existsById(String id);

    List<Transaction> findBySourceAccountIdOrDestinationAccountId(
            String sourceAccountId, String destinationAccountId);
}
