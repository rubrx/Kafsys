package com.kafsys.common.exception;

public class AccountNotActiveException extends KafsysException {

    public AccountNotActiveException(String accountId) {
        super("Account is not active: " + accountId, "ACCOUNT_NOT_ACTIVE");
    }
}
