package com.anibalxyz.features.auth.application;

import com.anibalxyz.core.Result;
import com.anibalxyz.features.auth.application.out.AuthResult;
import com.anibalxyz.features.auth.domain.*;
import com.anibalxyz.features.auth.domain.error.InvalidRefreshTokenError;
import com.anibalxyz.features.users.domain.UserId;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RefreshTokens {
  private static final Logger log = LoggerFactory.getLogger(RefreshTokens.class);
  private final Env env;
  private final Clock clock;
  private final RefreshTokenRepository refreshTokenRepository;
  private final MaintenancePolicy maintenancePolicy;
  private final JwtService jwtService;
  private final CreateRefreshToken createRefreshToken;

  public RefreshTokens(
      Env env,
      Clock clock,
      RefreshTokenRepository refreshTokenRepository,
      MaintenancePolicy maintenancePolicy,
      JwtService jwtService,
      CreateRefreshToken createRefreshToken) {
    this.env = env;
    this.clock = clock;
    this.refreshTokenRepository = refreshTokenRepository;
    this.maintenancePolicy = maintenancePolicy;
    this.jwtService = jwtService;
    this.createRefreshToken = createRefreshToken;
  }

  public Result<AuthResult, Error> execute(String refreshTokenString) {
    ZonedDateTime now = ZonedDateTime.now(clock);

    Optional<Instant> blocked = maintenancePolicy.blockedUntil(now);
    if (blocked.isPresent()) {
      return Result.failure(new Error.MaintenanceWindow(blocked.get()));
    }

    Instant expiryDate =
        maintenancePolicy.calculateExpiryDate(now, env.JWT_REFRESH_EXPIRATION_TIME_DAYS());
    return verifyRefreshToken(refreshTokenString, now.toInstant())
        .onSuccess(oldToken -> refreshTokenRepository.revoke(oldToken.tokenHash()))
        .map(
            oldToken ->
                new RotationResult(
                    oldToken.userId(), createRefreshToken.execute(oldToken.userId(), expiryDate)))
        .<Error>mapError(Error.InvalidToken::new)
        .map(
            rotationResult -> {
              String newAccessToken = jwtService.generateToken(rotationResult.userId().value());
              log.info("Tokens refreshed");
              return new AuthResult(newAccessToken, rotationResult.rawToken(), expiryDate);
            });
  }

  private Result<RefreshToken, InvalidRefreshTokenError> verifyRefreshToken(
      String rawToken, Instant now) {
    return RawToken.of(rawToken)
        .mapError(invalidRawTokenError -> InvalidRefreshTokenError.invalid())
        .map(TokenHash::of)
        .flatMap(this::findByHash)
        .flatMap(refreshToken -> refreshToken.checkIfExpired(now))
        .flatMap(RefreshToken::checkIfRevoked);
  }

  private Result<RefreshToken, InvalidRefreshTokenError> findByHash(TokenHash tokenHash) {
    return refreshTokenRepository
        .findByTokenHash(tokenHash)
        .<Result<RefreshToken, InvalidRefreshTokenError>>map(Result::success)
        .orElseGet(() -> Result.failure(InvalidRefreshTokenError.notFound()));
  }

  public sealed interface Error {
    record MaintenanceWindow(Instant availableFrom) implements Error {}

    record InvalidToken(InvalidRefreshTokenError error) implements Error {}
  }

  public interface Env {
    Duration JWT_REFRESH_EXPIRATION_TIME_DAYS();
  }

  public record RotationResult(UserId userId, RawToken rawToken) {}
}
