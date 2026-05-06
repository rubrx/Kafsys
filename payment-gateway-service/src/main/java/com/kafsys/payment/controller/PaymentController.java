package com.kafsys.payment.controller;

import com.kafsys.common.dto.ApiResponse;
import com.kafsys.common.dto.PagedResponse;
import com.kafsys.common.exception.ResourceNotFoundException;
import com.kafsys.payment.dto.PaymentResponse;
import com.kafsys.payment.entity.Payment;
import com.kafsys.payment.entity.PaymentStatus;
import com.kafsys.payment.repository.PaymentRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/payments")
@Tag(name = "Payments", description = "Payment lifecycle and settlement query endpoints")
public class PaymentController {

    private final PaymentRepository paymentRepository;

    public PaymentController(PaymentRepository paymentRepository) {
        this.paymentRepository = paymentRepository;
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get payment by ID")
    public ResponseEntity<ApiResponse<PaymentResponse>> getById(@PathVariable String id) {
        Payment payment = paymentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Payment", id));
        return ResponseEntity.ok(ApiResponse.ok(PaymentResponse.from(payment)));
    }

    @GetMapping("/transaction/{transactionId}")
    @Operation(summary = "Get payment by transaction ID")
    public ResponseEntity<ApiResponse<PaymentResponse>> getByTransactionId(
            @PathVariable String transactionId) {
        Payment payment = paymentRepository.findByTransactionId(transactionId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment for transaction", transactionId));
        return ResponseEntity.ok(ApiResponse.ok(PaymentResponse.from(payment)));
    }

    @GetMapping
    @Operation(summary = "List payments with pagination")
    public ResponseEntity<ApiResponse<PagedResponse<PaymentResponse>>> getAll(
            @RequestParam(required = false) String sourceAccountId,
            @RequestParam(required = false) PaymentStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Page<Payment> result;
        if (sourceAccountId != null) {
            result = paymentRepository.findBySourceAccountId(sourceAccountId, PageRequest.of(page, size));
        } else if (status != null) {
            result = paymentRepository.findByStatus(status, PageRequest.of(page, size));
        } else {
            result = paymentRepository.findAll(PageRequest.of(page, size));
        }

        PagedResponse<PaymentResponse> response = PagedResponse.of(
                result.getContent().stream().map(PaymentResponse::from).toList(),
                page, size, result.getTotalElements()
        );
        return ResponseEntity.ok(ApiResponse.ok(response));
    }
}
