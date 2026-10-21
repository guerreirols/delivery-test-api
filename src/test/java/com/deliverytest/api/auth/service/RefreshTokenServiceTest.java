package com.deliverytest.api.auth.service;

import com.deliverytest.api.auth.dto.AuthResponse;
import com.deliverytest.api.auth.dto.SessionTokens;
import com.deliverytest.api.auth.entity.RefreshToken;
import com.deliverytest.api.auth.exception.InvalidRefreshTokenException;
import com.deliverytest.api.auth.repository.RefreshTokenRepository;
import com.deliverytest.api.user.entity.User;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class RefreshTokenServiceTest {
    private static final String TOKEN = "a".repeat(43);
    private final RefreshTokenRepository repository = mock(RefreshTokenRepository.class);
    private final AccessTokenService access = mock(AccessTokenService.class);
    private final RefreshTokenService service = new RefreshTokenService(repository, access, Duration.ofDays(7));
    private final User user = User.builder().id("user-id").active(true).build();
    private final AuthResponse accessResponse = new AuthResponse("access", "Bearer", 3600);

    @ParameterizedTest
    @ValueSource(longs = {0, -1})
    void rejectsNonPositiveLifetime(long seconds) {
        assertThatThrownBy(() -> new RefreshTokenService(repository, access, Duration.ofSeconds(seconds)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void createsRandomSessionAndStoresOnlyHashWithExpiration() throws Exception {
        when(access.issue("user-id")).thenReturn(accessResponse);
        Instant before = Instant.now();

        SessionTokens result = service.create(user);

        ArgumentCaptor<RefreshToken> saved = ArgumentCaptor.forClass(RefreshToken.class);
        verify(repository).save(saved.capture());
        assertThat(result.access()).isSameAs(accessResponse);
        assertThat(result.refreshToken()).matches("[A-Za-z0-9_-]{43}");
        assertThat(saved.getValue().getTokenHash()).isEqualTo(hash(result.refreshToken()));
        assertThat(saved.getValue().getTokenHash()).isNotEqualTo(result.refreshToken());
        assertThat(saved.getValue().getUser()).isSameAs(user);
        assertThat(saved.getValue().getExpiresAt()).isBetween(before.plus(Duration.ofDays(7)),
                Instant.now().plus(Duration.ofDays(7)));
    }

    @Test
    void rotatesTokenAndIssuesAccessForSessionOwner() throws Exception {
        when(repository.rotate(anyString(), anyString(), any(Instant.class), any(Instant.class))).thenReturn(1);
        when(repository.findByTokenHash(anyString()))
                .thenAnswer(invocation -> Optional.of(new RefreshToken(invocation.getArgument(0), user, Instant.now())));
        when(access.issue("user-id")).thenReturn(accessResponse);

        SessionTokens result = service.refresh(TOKEN);

        ArgumentCaptor<String> replacementHash = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Instant> expiresAt = ArgumentCaptor.forClass(Instant.class);
        ArgumentCaptor<Instant> now = ArgumentCaptor.forClass(Instant.class);
        verify(repository).rotate(eq(hash(TOKEN)), replacementHash.capture(), expiresAt.capture(), now.capture());
        assertThat(result.refreshToken()).isNotEqualTo(TOKEN);
        assertThat(replacementHash.getValue()).isEqualTo(hash(result.refreshToken()));
        assertThat(Duration.between(now.getValue(), expiresAt.getValue())).isEqualTo(Duration.ofDays(7));
        assertThat(result.access()).isSameAs(accessResponse);
        verify(repository).findByTokenHash(replacementHash.getValue());
    }

    @Test
    void rejectedRotationNeverIssuesAccessOrReadsReplacement() {
        when(repository.rotate(anyString(), anyString(), any(Instant.class), any(Instant.class))).thenReturn(0);

        assertThatThrownBy(() -> service.refresh(TOKEN)).isInstanceOf(InvalidRefreshTokenException.class);
        verify(repository, never()).findByTokenHash(anyString());
        verifyNoInteractions(access);
    }

    @Test
    void missingReplacementNeverIssuesAccess() {
        when(repository.rotate(anyString(), anyString(), any(Instant.class), any(Instant.class))).thenReturn(1);
        when(repository.findByTokenHash(anyString())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.refresh(TOKEN)).isInstanceOf(InvalidRefreshTokenException.class);
        verifyNoInteractions(access);
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", " ", "invalid", "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa!"})
    void rejectsMalformedRefreshBeforeAccessingPersistence(String token) {
        assertThatThrownBy(() -> service.refresh(token)).isInstanceOf(InvalidRefreshTokenException.class);
        verifyNoInteractions(repository, access);
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", " ", "invalid"})
    void logoutIgnoresAbsentOrMalformedToken(String token) {
        service.logout(token);

        verifyNoInteractions(repository, access);
    }

    @Test
    void logoutRevokesOnlyPresentedTokenHash() throws Exception {
        service.logout(TOKEN);

        verify(repository).revoke(hash(TOKEN));
        verifyNoMoreInteractions(repository);
        verifyNoInteractions(access);
    }

    private String hash(String token) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(token.getBytes(StandardCharsets.US_ASCII)));
    }
}
