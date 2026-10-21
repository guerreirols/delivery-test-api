package com.deliverytest.api.auth.security;

import java.time.Duration;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

@Component
public class RefreshCookie {
    public static final String NAME = "refresh_token";
    private final Duration lifetime;
    private final boolean secure;
    private final String sameSite;

    public RefreshCookie(@Value("${security.refresh.lifetime:P7D}") Duration lifetime,
                         @Value("${security.refresh.cookie-secure:true}") boolean secure,
                         @Value("${security.refresh.cookie-same-site:Lax}") String sameSite) {
        if (lifetime.isNegative() || lifetime.isZero()) {
            throw new IllegalArgumentException("Refresh lifetime must be positive");
        }
        if (!Set.of("Strict", "Lax", "None").contains(sameSite) || (sameSite.equals("None") && !secure)) {
            throw new IllegalArgumentException("Invalid refresh cookie SameSite/Secure configuration");
        }
        this.lifetime = lifetime;
        this.secure = secure;
        this.sameSite = sameSite;
    }

    public String issue(String token) {
        return cookie(token, lifetime).toString();
    }

    public String clear() {
        return cookie("", Duration.ZERO).toString();
    }

    private ResponseCookie cookie(String value, Duration maxAge) {
        return ResponseCookie.from(NAME, value).httpOnly(true).secure(secure).sameSite(sameSite)
                .path("/auth").maxAge(maxAge).build();
    }
}
