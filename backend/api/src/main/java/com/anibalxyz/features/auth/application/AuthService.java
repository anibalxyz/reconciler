package com.anibalxyz.features.auth.application;

import com.anibalxyz.core.Result;
import com.anibalxyz.core.application.ValidationNotification;
import com.anibalxyz.features.auth.application.env.AuthEnvironment;
import com.anibalxyz.features.auth.application.in.LoginCommand;
import com.anibalxyz.features.auth.application.out.AuthResult;
import com.anibalxyz.features.auth.domain.RefreshToken;
import com.anibalxyz.features.auth.domain.error.InvalidCredentialsError;
import com.anibalxyz.features.auth.domain.error.InvalidRefreshTokenError;
import com.anibalxyz.features.users.application.GetUserByEmail;
import com.anibalxyz.features.users.domain.Email;
import com.anibalxyz.features.users.domain.Password;
import com.anibalxyz.features.users.domain.User;
import com.anibalxyz.features.users.domain.error.UserDomainError;
import com.anibalxyz.server.context.RequestContext;
import java.time.*;
import java.time.temporal.TemporalAdjusters;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// TODO: check if can divide this class as it is too overloaded
public class AuthService {
  private static final Logger log = LoggerFactory.getLogger(AuthService.class);
  private final AuthEnvironment env;
  private final Clock clock;
  private final GetUserByEmail getUserByEmail;
  private final JwtService jwtService;
  private final RefreshTokenService refreshTokenService;

  public AuthService(
      AuthEnvironment env,
      Clock clock,
      GetUserByEmail getUserByEmail,
      JwtService jwtService,
      RefreshTokenService refreshTokenService) {
    this.env = env;
    this.clock = clock;
    this.getUserByEmail = getUserByEmail;
    this.jwtService = jwtService;
    this.refreshTokenService = refreshTokenService;
  }

  private static Instant capAtNextFriday(ZonedDateTime now, Instant expiryDate) {
    Instant nextFriday20hs =
        now.with(TemporalAdjusters.nextOrSame(DayOfWeek.FRIDAY))
            .with(LocalTime.of(20, 0))
            .toInstant();

    expiryDate = expiryDate.isBefore(nextFriday20hs) ? expiryDate : nextFriday20hs;
    return expiryDate;
  }

  // NOTE: It could probably be domain logic. At the moment it will live here while
  //       completing other tasks
  public static Instant calculateExpiryDate(ZonedDateTime now, Duration expTimeDays) {
    Instant expiryDate = now.plus(expTimeDays).toInstant();

    expiryDate = capAtNextFriday(now, expiryDate);

    return expiryDate;
  }

  // TODO: its consumers should call it early
  public static Optional<Instant> blockedUntil(ZonedDateTime now) {
    DayOfWeek day = now.getDayOfWeek();
    LocalTime time = now.toLocalTime();

    // Blocked window: Friday 20:00 → Monday 08:00
    if (!((day == DayOfWeek.FRIDAY && time.isAfter(LocalTime.of(20, 0)))
        || day == DayOfWeek.SATURDAY
        || day == DayOfWeek.SUNDAY
        || (day == DayOfWeek.MONDAY && time.isBefore(LocalTime.of(8, 0))))) {
      return Optional.empty();
    }

    ZonedDateTime nextMonday =
        now.with(TemporalAdjusters.nextOrSame(DayOfWeek.MONDAY))
            .with(LocalTime.of(8, 0))
            .withNano(0);

    return Optional.of(nextMonday.toInstant());
  }

  public Result<AuthResult, AuthenticateUserError> authenticateUser(LoginCommand command) {
    ValidationNotification<UserDomainError> notification = new ValidationNotification<>();

    Email.validate(command.email()).onFailure(err -> notification.add("email", err));
    Password.validate(command.password()).onFailure(err -> notification.add("password", err));

    if (notification.hasErrors()) {
      return Result.failure(new AuthenticateUserError.ValidationFailed(notification));
    }

    Optional<Instant> blocked = blockedUntil(ZonedDateTime.now(clock));
    if (blocked.isPresent()) {
      return Result.failure(new AuthenticateUserError.MaintenanceWindow(blocked.get()));
    }

    var userResult = getUserByEmail.execute(command.email());

    return switch (userResult) {
      case Result.Failure(var ignored) ->
          Result.failure(
              new AuthenticateUserError.InvalidCredentials(new InvalidCredentialsError()));
      case Result.Success(User user) -> {
        if (!user.passwordMatches(command.password())) {
          yield Result.failure(
              new AuthenticateUserError.InvalidCredentials(new InvalidCredentialsError()));
        }

        RequestContext.setUserId(user.id().value());

        String accessToken = jwtService.generateToken(user.id().value());
        RefreshToken refreshToken =
            refreshTokenService.createRefreshToken(
                user,
                calculateExpiryDate(
                    ZonedDateTime.now(clock), env.JWT_REFRESH_EXPIRATION_TIME_DAYS()));
        log.info("User authenticated");
        yield Result.success(new AuthResult(accessToken, refreshToken));
      }
    };
  }

  public Result<AuthResult, RefreshTokensError> refreshTokens(String refreshTokenString) {
    Optional<Instant> blocked = blockedUntil(ZonedDateTime.now(clock));
    if (blocked.isPresent()) {
      return Result.failure(new RefreshTokensError.MaintenanceWindow(blocked.get()));
    }

    var rotationResult =
        refreshTokenService.verifyAndRotate(
            refreshTokenString,
            clock.instant(),
            calculateExpiryDate(ZonedDateTime.now(clock), env.JWT_REFRESH_EXPIRATION_TIME_DAYS()));

    return switch (rotationResult) {
      case Result.Failure(var invalidRefreshTokenError) ->
          Result.failure(new RefreshTokensError.InvalidToken(invalidRefreshTokenError));
      case Result.Success(var newRefreshToken) -> {
        String newAccessToken = jwtService.generateToken(newRefreshToken.user().id().value());
        log.info("Tokens refreshed");
        yield Result.success(new AuthResult(newAccessToken, newRefreshToken));
      }
    };
  }

  public sealed interface AuthenticateUserError {
    record InvalidCredentials(InvalidCredentialsError error) implements AuthenticateUserError {}

    record MaintenanceWindow(Instant availableFrom) implements AuthenticateUserError {}

    record ValidationFailed(ValidationNotification<UserDomainError> notification)
        implements AuthenticateUserError {}
  }

  public sealed interface RefreshTokensError {
    record MaintenanceWindow(Instant availableFrom) implements RefreshTokensError {}

    record InvalidToken(InvalidRefreshTokenError error) implements RefreshTokensError {}
  }
}
