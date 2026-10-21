package com.deliverytest.api.auth.exception;

public class InvalidRefreshTokenException extends RuntimeException {
    public InvalidRefreshTokenException() {
        super("error.auth.invalid-refresh-token");
    }
}
