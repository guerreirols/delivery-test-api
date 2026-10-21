package com.deliverytest.api.auth.repository;

import com.deliverytest.api.auth.entity.RefreshToken;
import java.time.Instant;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, String> {
    Optional<RefreshToken> findByTokenHash(String tokenHash);

    boolean existsByTokenHash(String tokenHash);

    @Modifying(clearAutomatically = true)
    @Query("update RefreshToken t set t.tokenHash = :replacementHash, t.expiresAt = :expiresAt "
            + "where t.tokenHash = :currentHash and t.expiresAt > :now and t.user.active = true")
    int rotate(String currentHash, String replacementHash, Instant expiresAt, Instant now);

    @Modifying
    @Transactional
    @Query("delete from RefreshToken t where t.tokenHash = :tokenHash")
    void revoke(String tokenHash);
}
