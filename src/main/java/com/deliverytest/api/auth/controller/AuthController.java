package com.deliverytest.api.auth.controller;

import com.deliverytest.api.auth.dto.AuthResponse;
import com.deliverytest.api.auth.dto.LoginRequest;
import com.deliverytest.api.auth.dto.RegisterRequest;
import com.deliverytest.api.auth.dto.SessionTokens;
import com.deliverytest.api.auth.security.RefreshCookie;
import com.deliverytest.api.auth.service.AuthService;
import com.deliverytest.api.auth.service.RefreshTokenService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(
        name = "Autenticação",
        description = "Endpoints para cadastro e autenticação de usuários"
)
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final RefreshTokenService refreshTokens;
    private final RefreshCookie refreshCookie;

    @Operation(
            summary = "Cadastrar usuário",
            description = "Cadastra um novo usuário com nome, e-mail e senha."
    )
    @PostMapping("/register")
    public ResponseEntity<Void> register(
            @Valid @RequestBody RegisterRequest request) {

        authService.register(request);
        return ResponseEntity.status(201).build();
    }

    @Operation(
            summary = "Autenticar usuário",
            description = "Autentica um usuário e retorna um token JWT de acesso."
    )
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(
            @Valid @RequestBody LoginRequest request) {
        return sessionResponse(authService.login(request));
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(
            @CookieValue(name = RefreshCookie.NAME, required = false) String token) {
        return sessionResponse(refreshTokens.refresh(token));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @CookieValue(name = RefreshCookie.NAME, required = false) String token) {
        refreshTokens.logout(token);
        return ResponseEntity.noContent().header(HttpHeaders.SET_COOKIE, refreshCookie.clear())
                .header(HttpHeaders.CACHE_CONTROL, "no-store").build();
    }

    private ResponseEntity<AuthResponse> sessionResponse(SessionTokens tokens) {
        return ResponseEntity.ok().header(HttpHeaders.SET_COOKIE, refreshCookie.issue(tokens.refreshToken()))
                .header(HttpHeaders.CACHE_CONTROL, "no-store").body(tokens.access());
    }
}
