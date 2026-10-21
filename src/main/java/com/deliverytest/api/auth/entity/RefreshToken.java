package com.deliverytest.api.auth.entity;

import com.deliverytest.api.user.entity.User;
import de.huxhorn.sulky.ulid.ULID;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "refresh_tokens")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RefreshToken {
    @Id
    @Column(length = 26, nullable = false, updatable = false)
    private String id;

    @Column(length = 64, nullable = false, unique = true)
    private String tokenHash;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private Instant expiresAt;

    public RefreshToken(String tokenHash, User user, Instant expiresAt) {
        this.id = new ULID().nextULID();
        this.tokenHash = tokenHash;
        this.user = user;
        this.expiresAt = expiresAt;
    }
}
