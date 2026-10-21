package com.deliverytest.api.auth.service;

import com.deliverytest.api.auth.dto.LoginRequest;
import com.deliverytest.api.auth.dto.RegisterRequest;
import com.deliverytest.api.auth.dto.SessionTokens;
import com.deliverytest.api.user.entity.User;
import com.deliverytest.api.user.exception.EmailAlreadyRegisteredException;
import com.deliverytest.api.user.repository.UserRegistrationStore;
import com.deliverytest.api.user.repository.UserRepository;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {
    private final UserRepository userRepository;
    private final UserRegistrationStore registrationStore;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final RefreshTokenService refreshTokens;

    public void register(RegisterRequest request) {
        String email = normalizeEmail(request.email());
        ensureEmailAvailable(email);
        User user = createUser(request, email);
        registrationStore.save(user);
        log.debug("Cadastro de usuário concluído");
    }

    public SessionTokens login(LoginRequest request) {
        User user = authenticate(request);
        SessionTokens tokens = refreshTokens.create(user);
        log.debug("Autenticação concluída");
        return tokens;
    }

    private void ensureEmailAvailable(String email) {
        if (userRepository.existsByEmail(email)) {
            throw new EmailAlreadyRegisteredException();
        }
    }

    private User createUser(RegisterRequest request, String email) {
        String encodedPassword = passwordEncoder.encode(request.password());
        return User.builder()
                .name(request.name().strip())
                .email(email)
                .password(encodedPassword)
                .active(true)
                .build();
    }

    private User authenticate(LoginRequest request) {
        String email = normalizeEmail(request.email());
        UsernamePasswordAuthenticationToken credentials =
                UsernamePasswordAuthenticationToken.unauthenticated(email, request.password());
        Authentication authentication = authenticationManager.authenticate(credentials);
        return (User) authentication.getPrincipal();
    }

    private String normalizeEmail(String email) {
        return email.strip().toLowerCase(Locale.ROOT);
    }
}
