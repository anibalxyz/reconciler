package com.anibalxyz.features.auth.application;

import com.anibalxyz.core.Result;
import com.anibalxyz.features.auth.application.out.AuthResult;
import com.anibalxyz.features.auth.domain.MaintenancePolicy;
import com.anibalxyz.features.auth.domain.error.InvalidRefreshTokenError;
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
  private final MaintenancePolicy maintenancePolicy;
  private final JwtService jwtService;
  private final RefreshTokenService refreshTokenService;

  public RefreshTokens(
      Env env,
      Clock clock,
      MaintenancePolicy maintenancePolicy,
      JwtService jwtService,
      RefreshTokenService refreshTokenService) {
    this.env = env;
    this.clock = clock;
    this.maintenancePolicy = maintenancePolicy;
    this.jwtService = jwtService;
    this.refreshTokenService = refreshTokenService;
  }

  public Result<AuthResult, Error> execute(String refreshTokenString) {
    Optional<Instant> blocked = maintenancePolicy.blockedUntil(ZonedDateTime.now(clock));
    if (blocked.isPresent()) {
      return Result.failure(new Error.MaintenanceWindow(blocked.get()));
    }

    Instant expiryDate =
        maintenancePolicy.calculateExpiryDate(
            ZonedDateTime.now(clock), env.JWT_REFRESH_EXPIRATION_TIME_DAYS());
    var rotationResult =
        refreshTokenService.verifyAndRotate(refreshTokenString, clock.instant(), expiryDate);

    return switch (rotationResult) {
      case Result.Failure(var invalidRefreshTokenError) ->
          Result.failure(new Error.InvalidToken(invalidRefreshTokenError));
      case Result.Success(var rotation) -> {
        String newAccessToken = jwtService.generateToken(rotation.userId().value());
        log.info("Tokens refreshed");
        yield Result.success(new AuthResult(newAccessToken, rotation.rawToken(), expiryDate));
      }
    };
  }

  public sealed interface Error {
    record MaintenanceWindow(Instant availableFrom) implements Error {}

    record InvalidToken(InvalidRefreshTokenError error) implements Error {}
  }

  public interface Env {
    Duration JWT_REFRESH_EXPIRATION_TIME_DAYS();
  }
}
