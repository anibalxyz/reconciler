package com.anibalxyz.features.auth.infra;

import com.anibalxyz.features.auth.domain.RefreshToken;
import com.anibalxyz.features.users.infra.UserEntity;
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

  @Column(name = "token", nullable = false, unique = true)
  private String token;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false)
  private UserEntity user;

  @Column(name = "expiry_date", nullable = false)
  private Instant expiryDate;

  @Column(name = "revoked", nullable = false)
  private boolean revoked;

  public RefreshTokenEntity() {}

  public static RefreshTokenEntity fromDomain(RefreshToken domain) {
    var entity = new RefreshTokenEntity();
    entity.id = domain.id();
    entity.token = domain.token();
    entity.user = UserEntity.fromDomain(domain.user());
    entity.expiryDate = domain.expiryDate();
    entity.revoked = domain.revoked();
    return entity;
  }

  public RefreshToken toDomain() {
    return new RefreshToken(id, token, user.toDomain(), expiryDate, revoked);
  }

  public Long getId() {
    return id;
  }

  public String getToken() {
    return token;
  }

  public UserEntity getUser() {
    return user;
  }

  public Instant getExpiryDate() {
    return expiryDate;
  }

  public boolean isRevoked() {
    return revoked;
  }
}
