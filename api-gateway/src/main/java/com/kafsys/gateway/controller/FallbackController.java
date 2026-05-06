package com.kafsys.gateway.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.Map;

@RestController
@RequestMapping("/fallback")
public class FallbackController {

    @RequestMapping("/transactions")
    public Mono<ResponseEntity<Map<String, Object>>> transactionFallback() {
        return Mono.just(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of(
                        "success", false,
                        "error", "Transaction Service is temporarily unavailable. Please retry shortly."
                )));
    }

    @RequestMapping("/accounts")
    public Mono<ResponseEntity<Map<String, Object>>> accountFallback() {
        return Mono.just(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of(
                        "success", false,
                        "error", "Account Service is temporarily unavailable. Please retry shortly."
                )));
    }

    @RequestMapping("/payments")
    public Mono<ResponseEntity<Map<String, Object>>> paymentFallback() {
        return Mono.just(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of(
                        "success", false,
                        "error", "Payment Gateway is temporarily unavailable. Please retry shortly."
                )));
    }
}
