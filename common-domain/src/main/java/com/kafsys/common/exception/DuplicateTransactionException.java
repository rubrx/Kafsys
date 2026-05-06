package com.kafsys.common.exception;

public class DuplicateTransactionException extends KafsysException {

    public DuplicateTransactionException(String transactionId) {
        super("Transaction already exists with idempotency key: " + transactionId, "DUPLICATE_TRANSACTION");
    }
}
