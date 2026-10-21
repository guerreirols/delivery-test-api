package com.deliverytest.api.auth.service;

import com.deliverytest.api.auth.dto.AuthResponse;
import java.time.Duration;
import java.time.Instant;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

@Service
public class AccessTokenService {
    public static final String ISSUER = "delivery-test";
    private final JwtEncoder encoder;
    private final Duration lifetime;

    public AccessTokenService(JwtEncoder encoder,
                        @Value("${security.jwt.lifetime:PT1H}") Duration lifetime) {
        if (lifetime.isNegative() || lifetime.isZero()) {
            throw new IllegalArgumentException("JWT lifetime must be positive");
        }
        this.encoder = encoder;
        this.lifetime = lifetime;
    }

    public AuthResponse issue(String userId) {
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder().issuer(ISSUER).subject(userId)
                .issuedAt(now).expiresAt(now.plus(lifetime)).build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        String token = encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
        return new AuthResponse(token, "Bearer", lifetime.toSeconds());
    }
}
