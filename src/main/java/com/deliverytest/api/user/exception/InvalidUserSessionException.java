package com.deliverytest.api.user.exception;

import org.springframework.security.core.AuthenticationException;

public class InvalidUserSessionException extends AuthenticationException {
    public InvalidUserSessionException() {
        super("error.auth.unauthorized");
    }
}
