package com.kafsys.transaction.controller;

import com.kafsys.common.dto.ApiResponse;
import com.kafsys.common.dto.PagedResponse;
import com.kafsys.common.enums.TransactionStatus;
import com.kafsys.common.enums.TransactionType;
import com.kafsys.transaction.dto.TransactionResponse;
import com.kafsys.transaction.dto.TransferRequest;
import com.kafsys.transaction.service.TransactionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/v1/transactions")
@Tag(name = "Transactions", description = "Fund transfer and transaction management endpoints")
public class TransactionController {

    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @PostMapping("/transfer")
    @Operation(
        summary = "Initiate fund transfer",
        description = "Creates an idempotent fund transfer. Provide a unique idempotency key to prevent duplicates."
    )
    public ResponseEntity<ApiResponse<TransactionResponse>> transfer(
            @Valid @RequestBody TransferRequest request) {
        TransactionResponse response = transactionService.initiateTransfer(request);
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(ApiResponse.ok(response, "Transaction initiated — processing asynchronously"));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get transaction by ID")
    public ResponseEntity<ApiResponse<TransactionResponse>> getById(
            @PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.ok(transactionService.getById(id)));
    }

    @GetMapping
    @Operation(summary = "List transactions with filters and pagination")
    public ResponseEntity<ApiResponse<PagedResponse<TransactionResponse>>> getTransactions(
            @Parameter(description = "Filter by source account ID")
            @RequestParam(required = false) String sourceAccountId,

            @Parameter(description = "Filter by status")
            @RequestParam(required = false) TransactionStatus status,

            @Parameter(description = "Filter by type")
            @RequestParam(required = false) TransactionType type,

            @Parameter(description = "Filter from date (ISO format)")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fromDate,

            @Parameter(description = "Filter to date (ISO format)")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime toDate,

            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        PagedResponse<TransactionResponse> result = transactionService
                .getTransactions(sourceAccountId, status, type, fromDate, toDate, page, size);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }
}
