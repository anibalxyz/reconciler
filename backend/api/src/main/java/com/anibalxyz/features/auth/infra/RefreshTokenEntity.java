package com.anibalxyz.features.auth.infra;

import com.anibalxyz.features.auth.domain.RefreshToken;
import com.anibalxyz.features.auth.domain.TokenHash;
import com.anibalxyz.features.users.domain.UserId;
import com.anibalxyz.features.users.infra.UserEntity;
import com.anibalxyz.features.users.infra.exception.CorruptedUserId;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "refresh_tokens")
public class RefreshTokenEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "token_hash", nullable = false, unique = true)
  private byte[] tokenHash;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false)
  private UserEntity user;

  @Column(name = "expiry_date", nullable = false)
  private Instant expiryDate;

  @Column(name = "revoked", nullable = false)
  private boolean revoked;

  protected RefreshTokenEntity() {}

  public RefreshTokenEntity(
      Long id, byte[] tokenHash, UserEntity user, Instant expiryDate, boolean revoked) {
    this.id = id;
    this.tokenHash = tokenHash;
    this.user = user;
    this.expiryDate = expiryDate;
    this.revoked = revoked;
  }

  public RefreshToken toDomain() {
    UserId userId =
        UserId.of(user.id()).orThrow(invalidUserIdError -> new CorruptedUserId(user.id()));
    return RefreshToken.reconstitute(
        TokenHash.reconstitute(tokenHash), userId, expiryDate, revoked);
  }
}
