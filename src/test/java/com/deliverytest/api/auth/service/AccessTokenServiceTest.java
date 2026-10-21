package com.deliverytest.api.auth.service;

import com.deliverytest.api.auth.dto.AuthResponse;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AccessTokenServiceTest {
    private final JwtEncoder encoder = mock(JwtEncoder.class);

    @ParameterizedTest
    @ValueSource(longs = {0, -1})
    void rejectsNonPositiveLifetime(long seconds) {
        assertThatThrownBy(() -> new AccessTokenService(encoder, Duration.ofSeconds(seconds)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void issuesSignedTokenWithTechnicalClaimsAndConfiguredExpiration() {
        Jwt jwt = mock(Jwt.class);
        when(jwt.getTokenValue()).thenReturn("signed-token");
        when(encoder.encode(any(JwtEncoderParameters.class))).thenReturn(jwt);
        Instant before = Instant.now();

        AuthResponse response = new AccessTokenService(encoder, Duration.ofMinutes(15)).issue("user-id");

        ArgumentCaptor<JwtEncoderParameters> captured = ArgumentCaptor.forClass(JwtEncoderParameters.class);
        verify(encoder).encode(captured.capture());
        assertThat(captured.getValue().getJwsHeader().getAlgorithm()).isEqualTo(MacAlgorithm.HS256);
        assertThat(captured.getValue().getClaims().getSubject()).isEqualTo("user-id");
        assertThat(captured.getValue().getClaims().getClaimAsString("iss")).isEqualTo(AccessTokenService.ISSUER);
        assertThat(captured.getValue().getClaims().getIssuedAt()).isBetween(before, Instant.now());
        assertThat(Duration.between(captured.getValue().getClaims().getIssuedAt(),
                captured.getValue().getClaims().getExpiresAt())).isEqualTo(Duration.ofMinutes(15));
        assertThat(captured.getValue().getClaims().getClaims()).containsOnlyKeys("sub", "iss", "iat", "exp");
        assertThat(response).isEqualTo(new AuthResponse("signed-token", "Bearer", 900));
    }

    @Test
    void propagatesEncoderFailureWithoutReturningToken() {
        IllegalStateException failure = new IllegalStateException("signing failed");
        when(encoder.encode(any(JwtEncoderParameters.class))).thenThrow(failure);

        assertThatThrownBy(() -> new AccessTokenService(encoder, Duration.ofHours(1)).issue("user-id"))
                .isSameAs(failure);
    }
}
