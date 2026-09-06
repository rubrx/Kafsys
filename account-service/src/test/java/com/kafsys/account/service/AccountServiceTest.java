package com.kafsys.account.service;

import com.kafsys.account.dto.AccountResponse;
import com.kafsys.account.dto.CreateAccountRequest;
import com.kafsys.account.entity.Account;
import com.kafsys.account.repository.AccountRepository;
import com.kafsys.common.enums.AccountStatus;
import com.kafsys.common.enums.KycStatus;
import com.kafsys.common.exception.AccountNotActiveException;
import com.kafsys.common.exception.InsufficientFundsException;
import com.kafsys.common.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    @Mock AccountRepository accounts;

    @InjectMocks AccountService service;

    private Account activeAccount(BigDecimal balance) {
        Account a = new Account();
        a.setOwnerId("owner-1");
        a.setOwnerName("Alice");
        a.setBalance(balance);
        a.setReservedBalance(BigDecimal.ZERO);
        a.setCurrency("USD");
        a.setStatus(AccountStatus.ACTIVE);
        a.setKycStatus(KycStatus.VERIFIED);
        return a;
    }

    @Test
    void createAccount_defaults_toPendingKycAndNotStarted() {
        when(accounts.save(any(Account.class))).thenAnswer(inv -> inv.getArgument(0));

        AccountResponse response = service.createAccount(
                new CreateAccountRequest("owner-1", "Alice", new BigDecimal("100.00"), "USD"));

        assertThat(response.status()).isEqualTo(AccountStatus.PENDING_KYC);
        assertThat(response.kycStatus()).isEqualTo(KycStatus.NOT_STARTED);
        assertThat(response.balance()).isEqualByComparingTo("100.00");
    }

    @Test
    void createAccount_nullInitialDeposit_defaultsToZero() {
        when(accounts.save(any(Account.class))).thenAnswer(inv -> inv.getArgument(0));

        AccountResponse response = service.createAccount(
                new CreateAccountRequest("owner-1", "Alice", null, "USD"));

        assertThat(response.balance()).isEqualByComparingTo("0");
    }

    @Test
    void getById_missing_throwsResourceNotFound() {
        when(accounts.findById("nope")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.getById("nope"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Account not found: nope");
    }

    @Test
    void reserveBalance_deductsFromAvailable_notFromBalance() {
        Account a = activeAccount(new BigDecimal("500.00"));
        when(accounts.findByIdForUpdate("acct-1")).thenReturn(Optional.of(a));

        service.reserveBalance("acct-1", new BigDecimal("120.50"));

        assertThat(a.getReservedBalance()).isEqualByComparingTo("120.50");
        assertThat(a.getBalance()).isEqualByComparingTo("500.00");
        assertThat(a.getAvailableBalance()).isEqualByComparingTo("379.50");
        verify(accounts).save(a);
    }

    @Test
    void reserveBalance_rejects_whenInactive() {
        Account a = activeAccount(new BigDecimal("500.00"));
        a.setStatus(AccountStatus.SUSPENDED);
        when(accounts.findByIdForUpdate("acct-1")).thenReturn(Optional.of(a));

        assertThatThrownBy(() -> service.reserveBalance("acct-1", BigDecimal.ONE))
                .isInstanceOf(AccountNotActiveException.class);
        verify(accounts, never()).save(any());
    }

    @Test
    void reserveBalance_rejects_whenInsufficientFunds() {
        Account a = activeAccount(new BigDecimal("50.00"));
        when(accounts.findByIdForUpdate("acct-1")).thenReturn(Optional.of(a));

        assertThatThrownBy(() -> service.reserveBalance("acct-1", new BigDecimal("100.00")))
                .isInstanceOf(InsufficientFundsException.class);
    }

    @Test
    void confirmDebit_reducesBothBalanceAndReservation() {
        Account a = activeAccount(new BigDecimal("500.00"));
        a.setReservedBalance(new BigDecimal("100.00"));
        when(accounts.findByIdForUpdate("acct-1")).thenReturn(Optional.of(a));

        service.confirmDebit("acct-1", new BigDecimal("100.00"));

        assertThat(a.getBalance()).isEqualByComparingTo("400.00");
        assertThat(a.getReservedBalance()).isEqualByComparingTo("0.00");
    }

    @Test
    void creditAccount_increasesBalance() {
        Account a = activeAccount(new BigDecimal("100.00"));
        when(accounts.findByIdForUpdate("acct-1")).thenReturn(Optional.of(a));

        service.creditAccount("acct-1", new BigDecimal("75.00"));

        assertThat(a.getBalance()).isEqualByComparingTo("175.00");
    }

    @Test
    void releaseReservation_decrementsReservedBalance() {
        Account a = activeAccount(new BigDecimal("100.00"));
        a.setReservedBalance(new BigDecimal("60.00"));
        when(accounts.findByIdForUpdate("acct-1")).thenReturn(Optional.of(a));

        service.releaseReservation("acct-1", new BigDecimal("40.00"));

        assertThat(a.getReservedBalance()).isEqualByComparingTo("20.00");
    }

    @Test
    void updateKycStatus_verified_activatesPendingAccount() {
        Account a = activeAccount(new BigDecimal("0"));
        a.setStatus(AccountStatus.PENDING_KYC);
        a.setKycStatus(KycStatus.PENDING);
        when(accounts.findById("acct-1")).thenReturn(Optional.of(a));

        AccountResponse response = service.updateKycStatus("acct-1", KycStatus.VERIFIED);

        assertThat(response.status()).isEqualTo(AccountStatus.ACTIVE);
        assertThat(response.kycStatus()).isEqualTo(KycStatus.VERIFIED);
    }

    @Test
    void updateKycStatus_rejected_suspendsAccount() {
        Account a = activeAccount(new BigDecimal("0"));
        a.setStatus(AccountStatus.PENDING_KYC);
        when(accounts.findById("acct-1")).thenReturn(Optional.of(a));

        service.updateKycStatus("acct-1", KycStatus.REJECTED);

        ArgumentCaptor<Account> saved = ArgumentCaptor.forClass(Account.class);
        verify(accounts).save(saved.capture());
        assertThat(saved.getValue().getStatus()).isEqualTo(AccountStatus.SUSPENDED);
    }
}
