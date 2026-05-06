package com.kafsys.alert.repository;

import com.kafsys.alert.entity.Alert;
import com.kafsys.common.enums.AlertType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AlertRepository extends JpaRepository<Alert, String> {

    Page<Alert> findByAccountId(String accountId, Pageable pageable);

    Page<Alert> findByAccountIdAndRead(String accountId, boolean read, Pageable pageable);

    Page<Alert> findByType(AlertType type, Pageable pageable);

    long countByAccountIdAndRead(String accountId, boolean read);
}
