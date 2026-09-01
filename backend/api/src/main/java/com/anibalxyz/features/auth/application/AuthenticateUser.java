package com.anibalxyz.features.auth.application;

import com.anibalxyz.core.Result;
import com.anibalxyz.core.application.ValidationNotification;
import com.anibalxyz.features.auth.application.in.LoginCommand;
import com.anibalxyz.features.auth.application.out.AuthResult;
import com.anibalxyz.features.auth.domain.MaintenancePolicy;
import com.anibalxyz.features.auth.domain.RawToken;
import com.anibalxyz.features.auth.domain.error.InvalidCredentialsError;
import com.anibalxyz.features.users.application.GetUserByEmail;
import com.anibalxyz.features.users.domain.Email;
import com.anibalxyz.features.users.domain.Password;
import com.anibalxyz.features.users.domain.User;
import com.anibalxyz.features.users.domain.UserId;
import com.anibalxyz.features.users.domain.error.UserDomainError;
import com.anibalxyz.server.context.RequestContext;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AuthenticateUser {
  private static final Logger log = LoggerFactory.getLogger(AuthenticateUser.class);
  private final Env env;
  private final Clock clock;
  private final MaintenancePolicy maintenancePolicy;
  private final GetUserByEmail getUserByEmail;
  private final JwtService jwtService;
  private final CreateRefreshToken createRefreshToken;

  public AuthenticateUser(
      Env env,
      Clock clock,
      MaintenancePolicy maintenancePolicy,
      GetUserByEmail getUserByEmail,
      JwtService jwtService,
      CreateRefreshToken createRefreshToken) {
    this.env = env;
    this.clock = clock;
    this.maintenancePolicy = maintenancePolicy;
    this.getUserByEmail = getUserByEmail;
    this.jwtService = jwtService;
    this.createRefreshToken = createRefreshToken;
  }

  public Result<AuthResult, Error> execute(LoginCommand command) {
    Optional<Instant> blocked = maintenancePolicy.blockedUntil(ZonedDateTime.now(clock));
    if (blocked.isPresent()) {
      return Result.failure(new Error.MaintenanceWindow(blocked.get()));
    }

    ValidationNotification<UserDomainError> notification = new ValidationNotification<>();

    Email.validate(command.email()).onFailure(err -> notification.add("email", err));
    Password.validate(command.password()).onFailure(err -> notification.add("password", err));

    if (notification.hasErrors()) {
      return Result.failure(new Error.ValidationFailed(notification));
    }

    var userResult = getUserByEmail.execute(command.email());

    return switch (userResult) {
      case Result.Failure(var ignored) ->
          Result.failure(new Error.InvalidCredentials(new InvalidCredentialsError()));
      case Result.Success(User user) -> {
        if (!user.passwordMatches(command.password())) {
          yield Result.failure(new Error.InvalidCredentials(new InvalidCredentialsError()));
        }

        UserId userId = user.id();

        RequestContext.setUserId(userId.value());

        String accessToken = jwtService.generateToken(userId.value());
        Instant expiryDate =
            maintenancePolicy.calculateExpiryDate(
                ZonedDateTime.now(clock), env.JWT_REFRESH_EXPIRATION_TIME_DAYS());
        RawToken refreshToken = createRefreshToken.execute(userId, expiryDate);
        log.info("User authenticated");
        yield Result.success(new AuthResult(accessToken, refreshToken, expiryDate));
      }
    };
  }

  public sealed interface Error {
    record InvalidCredentials(InvalidCredentialsError error) implements Error {}

    record MaintenanceWindow(Instant availableFrom) implements Error {}

    record ValidationFailed(ValidationNotification<UserDomainError> notification)
        implements Error {}
  }

  public interface Env {
    Duration JWT_REFRESH_EXPIRATION_TIME_DAYS();
  }
}
