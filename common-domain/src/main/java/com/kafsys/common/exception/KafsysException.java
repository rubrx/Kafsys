package com.kafsys.common.exception;

public class KafsysException extends RuntimeException {

    private final String errorCode;

    public KafsysException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
    }

    public KafsysException(String message, String errorCode, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
