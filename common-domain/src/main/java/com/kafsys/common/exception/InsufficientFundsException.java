package com.kafsys.common.exception;

import java.math.BigDecimal;

public class InsufficientFundsException extends KafsysException {

    public InsufficientFundsException(String accountId, BigDecimal requested, BigDecimal available) {
        super(
            String.format("Insufficient funds in account %s. Requested: %s, Available: %s",
                accountId, requested, available),
            "INSUFFICIENT_FUNDS"
        );
    }
}
