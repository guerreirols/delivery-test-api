package com.deliverytest.api.auth.security;

import com.deliverytest.api.auth.dto.AuthResponse;
import com.deliverytest.api.auth.service.AccessTokenService;
import com.deliverytest.api.user.entity.User;
import com.deliverytest.api.user.repository.UserRepository;
import java.time.Duration;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import javax.crypto.SecretKey;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.web.cors.CorsConfiguration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class SecurityConfigTest {
    private final SecurityConfig config = new SecurityConfig();

    @Test
    void ephemeralSigningKeysAreIndependentAndAtLeast256Bits() {
        SecretKey first = config.jwtSigningKey("");
        SecretKey second = config.jwtSigningKey("");

        assertThat(first.getEncoded()).hasSize(32).isNotEqualTo(second.getEncoded());
        assertThat(first.getAlgorithm()).isEqualTo("HmacSHA256");
    }

    @Test
    void usesConfiguredSigningKeyWithoutChangingBytes() {
        byte[] bytes = new byte[32];

        assertThat(config.jwtSigningKey(Base64.getEncoder().encodeToString(bytes)).getEncoded()).containsExactly(bytes);
    }

    @ParameterizedTest
    @ValueSource(strings = {"not-base64!", "c2hvcnQ="})
    void rejectsMalformedOrShortSigningKey(String secret) {
        assertThatThrownBy(() -> config.jwtSigningKey(secret)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void signedTokensRequireMatchingSigningKey() {
        SecretKey signingKey = config.jwtSigningKey("");
        AccessTokenService tokens = new AccessTokenService(config.jwtEncoder(signingKey), Duration.ofHours(1));
        AuthResponse response = tokens.issue("user-id");
        JwtDecoder trusted = config.jwtDecoder(signingKey);
        JwtDecoder untrusted = config.jwtDecoder(config.jwtSigningKey(""));

        assertThat(trusted.decode(response.accessToken()).getSubject()).isEqualTo("user-id");
        assertThatThrownBy(() -> untrusted.decode(response.accessToken())).isInstanceOf(JwtException.class);
    }

    @Test
    void loadsUserByEmailAndRejectsUnknownUser() {
        UserRepository repository = mock(UserRepository.class);
        User user = User.builder().email("maria@example.com").build();
        when(repository.findByEmail("maria@example.com")).thenReturn(Optional.of(user));
        when(repository.findByEmail("missing@example.com")).thenReturn(Optional.empty());
        UserDetailsService users = config.userDetailsService(repository);

        assertThat(users.loadUserByUsername("maria@example.com")).isSameAs(user);
        assertThatThrownBy(() -> users.loadUserByUsername("missing@example.com"))
                .isInstanceOf(UsernameNotFoundException.class);
    }

    @Test
    void authenticationRejectsDisabledUserEvenWithCorrectPassword() {
        PasswordEncoder passwords = config.passwordEncoder();
        User user = User.builder().email("maria@example.com").password(passwords.encode("secret123"))
                .active(false).build();
        AuthenticationManager manager = config.authenticationManager(email -> user, passwords);

        assertThatThrownBy(() -> manager.authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated("maria@example.com", "secret123")))
                .isInstanceOf(DisabledException.class);
    }

    @Test
    void corsAllowsCredentialsOnlyForConfiguredOriginsAndRequiredHeaders() {
        MockHttpServletRequest request = new MockHttpServletRequest("OPTIONS", "/auth/refresh");
        CorsConfiguration cors = config.corsConfigurationSource(
                new BrowserAuthPolicy(List.of("http://localhost:5173"))).getCorsConfiguration(request);

        assertThat(cors).isNotNull();
        assertThat(cors.getAllowCredentials()).isTrue();
        assertThat(cors.checkOrigin("http://localhost:5173")).isEqualTo("http://localhost:5173");
        assertThat(cors.checkOrigin("https://hostile.example")).isNull();
        assertThat(cors.checkHeaders(List.of("X-Refresh-Request", "Content-Type")))
                .containsExactly("X-Refresh-Request", "Content-Type");
        assertThat(cors.checkHttpMethod(org.springframework.http.HttpMethod.POST)).isNotNull();
    }
}
