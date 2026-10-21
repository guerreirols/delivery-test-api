package com.deliverytest.api.auth.security;

import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class BrowserAuthPolicy {
    private final List<String> allowedOrigins;

    public BrowserAuthPolicy(@Value("${security.cors.allowed-origins:http://localhost:5173,http://localhost:3000}")
                             List<String> allowedOrigins) {
        if (allowedOrigins.isEmpty() || allowedOrigins.stream().anyMatch(origin -> origin.contains("*"))) {
            throw new IllegalArgumentException("CORS requires explicit origins");
        }
        this.allowedOrigins = List.copyOf(allowedOrigins);
    }

    public List<String> allowedOrigins() {
        return allowedOrigins;
    }

    public boolean allows(String origin) {
        return origin == null || allowedOrigins.contains(origin);
    }
}
