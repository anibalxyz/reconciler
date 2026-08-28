package com.anibalxyz.features.auth.infra;

import com.anibalxyz.features.auth.domain.RefreshToken;
import com.anibalxyz.features.auth.domain.RefreshTokenRepository;
import com.anibalxyz.features.auth.domain.TokenHash;
import com.anibalxyz.features.users.infra.UserEntity;
import com.anibalxyz.persistence.EntityManagerProvider;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import java.util.Optional;

public class JpaRefreshTokenRepository implements RefreshTokenRepository {

  private final EntityManagerProvider provider;

  public JpaRefreshTokenRepository(EntityManagerProvider provider) {
    this.provider = provider;
  }

  @SuppressWarnings("resource")
  private RefreshTokenEntity fromDomain(RefreshToken domain) {
    return new RefreshTokenEntity(
        null,
        domain.tokenHash().value(),
        em().getReference(UserEntity.class, domain.userId().value()),
        domain.expiryDate(),
        domain.isRevoked());
  }

  private EntityManager em() {
    return provider.get();
  }

  @Override
  @SuppressWarnings("resource")
  public Optional<RefreshToken> findByTokenHash(TokenHash tokenHash) {
    try {
      RefreshTokenEntity entity =
          em().createQuery(
                  "SELECT rt FROM RefreshTokenEntity rt WHERE rt.tokenHash = :tokenHash",
                  RefreshTokenEntity.class)
              .setParameter("tokenHash", tokenHash.value())
              .getSingleResult();
      return Optional.of(entity.toDomain());
    } catch (NoResultException e) {
      return Optional.empty();
    }
  }

  @Override
  @SuppressWarnings("resource")
  public void persist(RefreshToken refreshToken) {
    RefreshTokenEntity entity = fromDomain(refreshToken);
    em().persist(entity);
  }

  @Override
  @SuppressWarnings("resource")
  public void revoke(TokenHash tokenHash) {
    em().createQuery(
            "UPDATE RefreshTokenEntity rt SET rt.revoked = true WHERE rt.tokenHash = :tokenHash")
        .setParameter("tokenHash", tokenHash.value())
        .executeUpdate();
  }

  @Override
  @SuppressWarnings("resource")
  public int deleteExpiredTokens() {
    // TODO: should pass the 'now' of the application, not using the database
    return em().createQuery(
            "DELETE FROM RefreshTokenEntity rt WHERE rt.expiryDate < CURRENT_TIMESTAMP")
        .executeUpdate();
  }
}
