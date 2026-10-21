package com.deliverytest.api.user.exception;

public class EmailAlreadyRegisteredException extends RuntimeException {
    public EmailAlreadyRegisteredException() {
        super("error.auth.email-already-registered");
    }

    public EmailAlreadyRegisteredException(Throwable cause) {
        super("error.auth.email-already-registered", cause);
    }
}
