package com.deliverytest.api.auth.service;

import com.deliverytest.api.auth.dto.SessionTokens;
import com.deliverytest.api.auth.entity.RefreshToken;
import com.deliverytest.api.auth.exception.InvalidRefreshTokenException;
import com.deliverytest.api.auth.repository.RefreshTokenRepository;
import com.deliverytest.api.user.entity.User;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
public class RefreshTokenService {
    private final RefreshTokenRepository repository;
    private final AccessTokenService accessTokens;
    private final Duration lifetime;
    private final SecureRandom random = new SecureRandom();

    public RefreshTokenService(RefreshTokenRepository repository, AccessTokenService accessTokens,
                               @Value("${security.refresh.lifetime:P7D}") Duration lifetime) {
        if (lifetime.isNegative() || lifetime.isZero()) {
            throw new IllegalArgumentException("Refresh lifetime must be positive");
        }
        this.repository = repository;
        this.accessTokens = accessTokens;
        this.lifetime = lifetime;
    }

    @Transactional
    public SessionTokens create(User user) {
        String token = generate();
        Instant expiresAt = Instant.now().plus(lifetime);
        repository.save(new RefreshToken(hash(token), user, expiresAt));
        SessionTokens tokens = new SessionTokens(accessTokens.issue(user.getId()), token);
        log.debug("Nova sessão de renovação preparada");
        return tokens;
    }

    @Transactional
    public SessionTokens refresh(String currentToken) {
        String currentHash = hash(currentToken);
        String replacement = generate();
        rotate(currentHash, replacement);
        String userId = findUserId(hash(replacement));
        SessionTokens tokens = new SessionTokens(accessTokens.issue(userId), replacement);
        log.debug("Rotação de sessão de renovação preparada");
        return tokens;
    }

    private void rotate(String currentHash, String replacement) {
        Instant now = Instant.now();
        int updated = repository.rotate(currentHash, hash(replacement), now.plus(lifetime), now);
        if (updated != 1) {
            throw new InvalidRefreshTokenException();
        }
    }

    public void logout(String token) {
        if (isTokenFormatValid(token)) {
            repository.revoke(hash(token));
            log.debug("Solicitação de revogação de sessão processada");
        }
    }

    private String findUserId(String tokenHash) {
        RefreshToken token = repository.findByTokenHash(tokenHash).orElseThrow(InvalidRefreshTokenException::new);
        return token.getUser().getId();
    }

    private String generate() {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hash(String token) {
        if (!isTokenFormatValid(token)) {
            throw new InvalidRefreshTokenException();
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(token.getBytes(StandardCharsets.US_ASCII)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 unavailable", exception);
        }
    }

    private boolean isTokenFormatValid(String token) {
        return token != null && token.matches("[A-Za-z0-9_-]{43}");
    }
}
