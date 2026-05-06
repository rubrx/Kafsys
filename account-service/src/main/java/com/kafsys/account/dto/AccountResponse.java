package com.kafsys.account.dto;

import com.kafsys.account.entity.Account;
import com.kafsys.common.enums.AccountStatus;
import com.kafsys.common.enums.KycStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record AccountResponse(
        String id,
        String accountNumber,
        String ownerId,
        String ownerName,
        BigDecimal balance,
        BigDecimal availableBalance,
        BigDecimal reservedBalance,
        String currency,
        AccountStatus status,
        KycStatus kycStatus,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static AccountResponse from(Account account) {
        return new AccountResponse(
                account.getId(),
                account.getAccountNumber(),
                account.getOwnerId(),
                account.getOwnerName(),
                account.getBalance(),
                account.getAvailableBalance(),
                account.getReservedBalance(),
                account.getCurrency(),
                account.getStatus(),
                account.getKycStatus(),
                account.getCreatedAt(),
                account.getUpdatedAt()
        );
    }
}
