package com.kafsys.account.controller;

import com.kafsys.account.dto.AccountResponse;
import com.kafsys.account.dto.CreateAccountRequest;
import com.kafsys.account.service.AccountService;
import com.kafsys.common.dto.ApiResponse;
import com.kafsys.common.dto.PagedResponse;
import com.kafsys.common.enums.AccountStatus;
import com.kafsys.common.enums.KycStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/accounts")
@Tag(name = "Accounts", description = "Account management, balance tracking, and KYC verification")
public class AccountController {

    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @PostMapping
    @Operation(summary = "Create a new bank account")
    public ResponseEntity<ApiResponse<AccountResponse>> create(
            @Valid @RequestBody CreateAccountRequest request) {
        AccountResponse response = accountService.createAccount(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(response));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get account by ID")
    public ResponseEntity<ApiResponse<AccountResponse>> getById(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.ok(accountService.getById(id)));
    }

    @GetMapping
    @Operation(summary = "List all accounts with pagination")
    public ResponseEntity<ApiResponse<PagedResponse<AccountResponse>>> getAll(
            @RequestParam(required = false) String ownerId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        PagedResponse<AccountResponse> result = ownerId != null
                ? accountService.getAccountsByOwner(ownerId, page, size)
                : accountService.getAllAccounts(page, size);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    @PutMapping("/{id}/status")
    @Operation(summary = "Update account status (ACTIVE, SUSPENDED, CLOSED)")
    public ResponseEntity<ApiResponse<AccountResponse>> updateStatus(
            @PathVariable String id,
            @RequestParam AccountStatus status) {
        return ResponseEntity.ok(ApiResponse.ok(accountService.updateStatus(id, status)));
    }

    @PutMapping("/{id}/kyc")
    @Operation(summary = "Update KYC verification status")
    public ResponseEntity<ApiResponse<AccountResponse>> updateKyc(
            @PathVariable String id,
            @RequestParam KycStatus kycStatus) {
        return ResponseEntity.ok(ApiResponse.ok(accountService.updateKycStatus(id, kycStatus)));
    }

    @GetMapping("/{id}/balance")
    @Operation(summary = "Get account balance summary")
    public ResponseEntity<ApiResponse<AccountResponse>> getBalance(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.ok(accountService.getById(id)));
    }
}
