package com.kafsys.account.service;

import com.kafsys.account.dto.AccountResponse;
import com.kafsys.account.dto.CreateAccountRequest;
import com.kafsys.account.entity.Account;
import com.kafsys.account.repository.AccountRepository;
import com.kafsys.common.dto.PagedResponse;
import com.kafsys.common.enums.AccountStatus;
import com.kafsys.common.enums.KycStatus;
import com.kafsys.common.exception.AccountNotActiveException;
import com.kafsys.common.exception.InsufficientFundsException;
import com.kafsys.common.exception.ResourceNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
public class AccountService {

    private static final Logger log = LoggerFactory.getLogger(AccountService.class);

    private final AccountRepository accountRepository;

    public AccountService(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    @Transactional
    public AccountResponse createAccount(CreateAccountRequest request) {
        Account account = new Account();
        account.setOwnerId(request.ownerId());
        account.setOwnerName(request.ownerName());
        account.setBalance(request.initialDeposit() != null ? request.initialDeposit() : BigDecimal.ZERO);
        account.setCurrency(request.currency());
        account.setStatus(AccountStatus.PENDING_KYC);
        account.setKycStatus(KycStatus.NOT_STARTED);
        account = accountRepository.save(account);
        log.info("Account created: id={} owner={}", account.getId(), account.getOwnerId());
        return AccountResponse.from(account);
    }

    @Cacheable(value = "accounts", key = "#id")
    @Transactional(readOnly = true)
    public AccountResponse getById(String id) {
        return accountRepository.findById(id)
                .map(AccountResponse::from)
                .orElseThrow(() -> new ResourceNotFoundException("Account", id));
    }

    @Transactional(readOnly = true)
    public PagedResponse<AccountResponse> getAccountsByOwner(String ownerId, int page, int size) {
        Page<Account> result = accountRepository.findByOwnerId(ownerId, PageRequest.of(page, size));
        return PagedResponse.of(
                result.getContent().stream().map(AccountResponse::from).toList(),
                page, size, result.getTotalElements()
        );
    }

    @Transactional(readOnly = true)
    public PagedResponse<AccountResponse> getAllAccounts(int page, int size) {
        Page<Account> result = accountRepository.findAll(PageRequest.of(page, size));
        return PagedResponse.of(
                result.getContent().stream().map(AccountResponse::from).toList(),
                page, size, result.getTotalElements()
        );
    }

    @Transactional
    @CacheEvict(value = "accounts", key = "#id")
    public AccountResponse updateStatus(String id, AccountStatus newStatus) {
        Account account = accountRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Account", id));
        account.setStatus(newStatus);
        accountRepository.save(account);
        log.info("Account status updated: id={} newStatus={}", id, newStatus);
        return AccountResponse.from(account);
    }

    @Transactional
    @CacheEvict(value = "accounts", key = "#id")
    public AccountResponse updateKycStatus(String id, KycStatus kycStatus) {
        Account account = accountRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Account", id));
        account.setKycStatus(kycStatus);
        if (kycStatus == KycStatus.VERIFIED && account.getStatus() == AccountStatus.PENDING_KYC) {
            account.setStatus(AccountStatus.ACTIVE);
        } else if (kycStatus == KycStatus.REJECTED) {
            account.setStatus(AccountStatus.SUSPENDED);
        }
        accountRepository.save(account);
        log.info("Account KYC updated: id={} kycStatus={}", id, kycStatus);
        return AccountResponse.from(account);
    }

    @Transactional
    public void reserveBalance(String accountId, BigDecimal amount) {
        Account account = accountRepository.findByIdForUpdate(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Account", accountId));

        if (account.getStatus() != AccountStatus.ACTIVE) {
            throw new AccountNotActiveException(accountId);
        }
        if (account.getAvailableBalance().compareTo(amount) < 0) {
            throw new InsufficientFundsException(accountId, amount, account.getAvailableBalance());
        }

        account.setReservedBalance(account.getReservedBalance().add(amount));
        accountRepository.save(account);
    }

    @Transactional
    public void confirmDebit(String accountId, BigDecimal amount) {
        Account account = accountRepository.findByIdForUpdate(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Account", accountId));

        account.setBalance(account.getBalance().subtract(amount));
        account.setReservedBalance(account.getReservedBalance().subtract(amount));
        accountRepository.save(account);
    }

    @Transactional
    public void creditAccount(String accountId, BigDecimal amount) {
        Account account = accountRepository.findByIdForUpdate(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Account", accountId));

        account.setBalance(account.getBalance().add(amount));
        accountRepository.save(account);
    }

    @Transactional
    public void releaseReservation(String accountId, BigDecimal amount) {
        Account account = accountRepository.findByIdForUpdate(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Account", accountId));

        account.setReservedBalance(account.getReservedBalance().subtract(amount));
        accountRepository.save(account);
    }
}
