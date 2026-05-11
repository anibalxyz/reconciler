package com.anibalxyz.features.auth.application;

import com.anibalxyz.features.auth.application.env.AuthEnvironment;
import com.anibalxyz.features.auth.application.in.LoginCommand;
import com.anibalxyz.features.auth.application.out.AuthResult;
import com.anibalxyz.features.auth.domain.RefreshToken;
import com.anibalxyz.features.auth.domain.error.InvalidCredentialsError;
import com.anibalxyz.features.auth.domain.error.InvalidRefreshTokenError;
import com.anibalxyz.features.common.Result;
import com.anibalxyz.features.common.application.ValidationNotification;
import com.anibalxyz.features.users.application.UserService;
import com.anibalxyz.features.users.domain.Email;
import com.anibalxyz.features.users.domain.PasswordHash;
import com.anibalxyz.features.users.domain.User;
import com.anibalxyz.features.users.domain.error.InvalidEmailError;
import com.anibalxyz.features.users.domain.error.InvalidPasswordError;
import com.anibalxyz.features.users.domain.error.UserDomainError;
import com.anibalxyz.server.context.RequestContext;
import java.time.*;
import java.time.temporal.TemporalAdjusters;
import java.util.Optional;

// TODO: check if can divide this class as it is too overloaded
public class AuthService {
  private final AuthEnvironment env;
  private final Clock clock;
  private final UserService userService;
  private final JwtService jwtService;
  private final RefreshTokenService refreshTokenService;

  public AuthService(
      AuthEnvironment env,
      Clock clock,
      UserService userService,
      JwtService jwtService,
      RefreshTokenService refreshTokenService) {
    this.env = env;
    this.clock = clock;
    this.userService = userService;
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

    Result<Void, InvalidEmailError> emailValidation = Email.validateRaw(command.email());
    if (emailValidation.isFailure()) notification.add("email", emailValidation.getError());

    Result<Void, InvalidPasswordError> passwordValidation =
        PasswordHash.validate(command.password());
    if (passwordValidation.isFailure()) notification.add("password", passwordValidation.getError());

    if (notification.hasErrors()) {
      return Result.failure(new AuthenticateUserError.ValidationFailed(notification));
    }

    Optional<Instant> blocked = blockedUntil(ZonedDateTime.now(clock));
    if (blocked.isPresent()) {
      return Result.failure(new AuthenticateUserError.MaintenanceWindow(blocked.get()));
    }

    Result<User, ?> userResult = userService.getUserByEmail(command.email());
    if (userResult.isFailure()) {
      return Result.failure(
          new AuthenticateUserError.InvalidCredentials(new InvalidCredentialsError()));
    }

    User user = userResult.getValue();
    if (!user.passwordHash().matches(command.password())) {
      return Result.failure(
          new AuthenticateUserError.InvalidCredentials(new InvalidCredentialsError()));
    }

      RequestContext.setUserId(user.id());

    String accessToken = jwtService.generateToken(user.id());
    RefreshToken refreshToken =
        refreshTokenService.createRefreshToken(
            user,
            calculateExpiryDate(ZonedDateTime.now(clock), env.JWT_REFRESH_EXPIRATION_TIME_DAYS()));
    return Result.success(new AuthResult(accessToken, refreshToken));
  }

  public Result<AuthResult, RefreshTokensError> refreshTokens(String refreshTokenString) {
    // TODO: can refactor this 4 lines
    Optional<Instant> blocked = blockedUntil(ZonedDateTime.now(clock));
    if (blocked.isPresent()) {
      return Result.failure(new RefreshTokensError.MaintenanceWindow(blocked.get()));
    }

    Result<RefreshToken, InvalidRefreshTokenError> rotationResult =
        refreshTokenService.verifyAndRotate(
            refreshTokenString,
            clock.instant(),
            calculateExpiryDate(ZonedDateTime.now(clock), env.JWT_REFRESH_EXPIRATION_TIME_DAYS()));
    if (rotationResult.isFailure()) {
      return Result.failure(new RefreshTokensError.InvalidToken(rotationResult.getError()));
    }

    RefreshToken newRefreshToken = rotationResult.getValue();
    String newAccessToken = jwtService.generateToken(newRefreshToken.user().id());
    return Result.success(new AuthResult(newAccessToken, newRefreshToken));
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
