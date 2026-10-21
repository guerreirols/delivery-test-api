package com.deliverytest.api.auth.service;

import com.deliverytest.api.auth.dto.AuthResponse;
import com.deliverytest.api.auth.dto.LoginRequest;
import com.deliverytest.api.auth.dto.RegisterRequest;
import com.deliverytest.api.auth.dto.SessionTokens;
import com.deliverytest.api.user.entity.User;
import com.deliverytest.api.user.exception.EmailAlreadyRegisteredException;
import com.deliverytest.api.user.repository.UserRegistrationStore;
import com.deliverytest.api.user.repository.UserRepository;
import java.util.Locale;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AuthServiceTest {
    private final UserRepository users = mock(UserRepository.class);
    private final UserRegistrationStore store = mock(UserRegistrationStore.class);
    private final PasswordEncoder passwords = mock(PasswordEncoder.class);
    private final AuthenticationManager authentication = mock(AuthenticationManager.class);
    private final RefreshTokenService refreshTokens = mock(RefreshTokenService.class);
    private final AuthService service = new AuthService(users, store, passwords, authentication, refreshTokens);

    @Test
    void registersNormalizedUserWithEncodedPasswordAndEnabledAccount() {
        when(passwords.encode(" secret123 ")).thenReturn("encoded-password");

        service.register(new RegisterRequest(" Maria ", " MARIA@Example.com ", " secret123 "));

        ArgumentCaptor<User> captured = ArgumentCaptor.forClass(User.class);
        verify(store).save(captured.capture());
        assertThat(captured.getValue().getName()).isEqualTo("Maria");
        assertThat(captured.getValue().getEmail()).isEqualTo("maria@example.com");
        assertThat(captured.getValue().getPassword()).isEqualTo("encoded-password");
        assertThat(captured.getValue().isEnabled()).isTrue();
        verify(users).existsByEmail("maria@example.com");
    }

    @Test
    void duplicateEmailDoesNotHashPasswordOrPersistUser() {
        when(users.existsByEmail("maria@example.com")).thenReturn(true);

        assertThatThrownBy(() -> service.register(new RegisterRequest("Maria", "MARIA@example.com", "secret123")))
                .isInstanceOf(EmailAlreadyRegisteredException.class);

        verifyNoInteractions(passwords, store, refreshTokens);
    }

    @Test
    void passwordEncodingFailureDoesNotPersistUser() {
        IllegalStateException failure = new IllegalStateException("encoder unavailable");
        when(passwords.encode("secret123")).thenThrow(failure);

        assertThatThrownBy(() -> service.register(new RegisterRequest("Maria", "maria@example.com", "secret123")))
                .isSameAs(failure);
        verifyNoInteractions(store);
    }

    @Test
    void authenticatesNormalizedEmailAndIssuesSessionForAuthenticatedUser() {
        User user = User.builder().id("user-id").email("maria@example.com").active(true).build();
        Authentication authenticated = UsernamePasswordAuthenticationToken.authenticated(user, null, user.getAuthorities());
        SessionTokens expected = new SessionTokens(new AuthResponse("access", "Bearer", 3600), "refresh");
        when(authentication.authenticate(any(Authentication.class))).thenReturn(authenticated);
        when(refreshTokens.create(user)).thenReturn(expected);

        SessionTokens result = service.login(new LoginRequest(" MARIA@Example.com ", "secret123"));

        ArgumentCaptor<Authentication> credentials = ArgumentCaptor.forClass(Authentication.class);
        verify(authentication).authenticate(credentials.capture());
        assertThat(credentials.getValue().getName()).isEqualTo("maria@example.com");
        assertThat(credentials.getValue().getCredentials()).isEqualTo("secret123");
        assertThat(credentials.getValue().isAuthenticated()).isFalse();
        assertThat(result).isSameAs(expected);
    }

    @Test
    void invalidCredentialsNeverCreateRefreshSession() {
        BadCredentialsException failure = new BadCredentialsException("invalid credentials");
        when(authentication.authenticate(any(Authentication.class))).thenThrow(failure);

        assertThatThrownBy(() -> service.login(new LoginRequest("maria@example.com", "wrong"))).isSameAs(failure);
        verifyNoInteractions(refreshTokens);
    }

    @Test
    void normalizationDoesNotDependOnMachineLocale() {
        Locale previous = Locale.getDefault();
        try {
            Locale.setDefault(Locale.forLanguageTag("tr-TR"));
            service.register(new RegisterRequest("Iara", "IARA@EXAMPLE.COM", "secret123"));
            verify(users).existsByEmail("iara@example.com");
        } finally {
            Locale.setDefault(previous);
        }
    }
}
