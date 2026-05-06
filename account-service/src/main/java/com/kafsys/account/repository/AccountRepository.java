package com.kafsys.account.repository;

import com.kafsys.account.entity.Account;
import com.kafsys.common.enums.AccountStatus;
import com.kafsys.common.enums.KycStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;
import java.util.Optional;

@Repository
public interface AccountRepository extends JpaRepository<Account, String> {

    Optional<Account> findByAccountNumber(String accountNumber);

    Page<Account> findByOwnerId(String ownerId, Pageable pageable);

    Page<Account> findByStatus(AccountStatus status, Pageable pageable);

    // Pessimistic write lock for balance operations — prevents concurrent overdraft
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM Account a WHERE a.id = :id")
    Optional<Account> findByIdForUpdate(@Param("id") String id);

    @Query("SELECT COUNT(a) FROM Account a WHERE a.ownerId = :ownerId AND a.status != 'CLOSED'")
    long countActiveAccountsByOwner(@Param("ownerId") String ownerId);

    boolean existsByAccountNumber(String accountNumber);

    Page<Account> findByKycStatus(KycStatus kycStatus, Pageable pageable);
}
