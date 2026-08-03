package com.anibalxyz.features.users.application;

import com.anibalxyz.core.Result;
import com.anibalxyz.core.application.ValidationNotification;
import com.anibalxyz.features.users.application.in.UpdateUserCommand;
import com.anibalxyz.features.users.domain.*;
import com.anibalxyz.features.users.domain.error.*;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UpdateUserById {

  private static final Logger log = LoggerFactory.getLogger(UpdateUserById.class);

  private final Env env;
  private final UserRepository userRepository;

  public UpdateUserById(Env env, UserRepository userRepository) {
    this.env = env;
    this.userRepository = userRepository;
  }

  public Result<User, UpdateUserById.UpdateUserByIdError> execute(
      Integer id, UpdateUserCommand command) {
    // Fail-fast: avoids unnecessary expensive operations (e.g., bcrypt) when all fields are absent.
    // Redundant with VO null checks, but the performance trade-off justifies it.
    if (!command.hasAtLeastOneField()) {
      return Result.failure(new UpdateUserById.UpdateUserByIdError.EmptyCommand());
    }

    // TODO: I/O operation should be the last one
    Optional<User> userOptional = userRepository.findById(id);
    if (userOptional.isEmpty()) {
      return Result.failure(
          new UpdateUserById.UpdateUserByIdError.NotFound(UserNotFoundError.byId(id)));
    }
    User user = userOptional.get();

    ValidationNotification<UserDomainError> notification = new ValidationNotification<>();

    switch (Name.of(command.name())) {
      case Result.Failure(var err) -> {
        if (!(err.getReason() instanceof InvalidNameError.Reason.Absent)) {
          notification.add("name", err);
        }
      }
      case Result.Success(var name) -> user = user.withName(name);
    }

    switch (Email.of(command.email())) {
      case Result.Failure(var err) -> {
        if (!(err.getReason() instanceof InvalidEmailError.Reason.Absent)) {
          notification.add("email", err);
        }
      }
      case Result.Success(var email) -> {
        if (user.email().equals(email)) break;

        if (userRepository.findByEmail(email).isPresent()) {
          notification.add("email", new EmailAlreadyTakenError());
        } else {
          user = user.withEmail(email);
        }
      }
    }

    switch (PasswordHash.generate(command.password(), env.BCRYPT_LOG_ROUNDS())) {
      case Result.Failure(var err) -> {
        if (!(err.getReason() instanceof InvalidPasswordError.Reason.Absent)) {
          notification.add("password", err);
        }
      }
      case Result.Success(var pass) -> user = user.withPasswordHash(pass);
    }

    if (notification.hasErrors()) {
      return Result.failure(new UpdateUserById.UpdateUserByIdError.ValidationFailed(notification));
    }

    log.info("User updated");
    return Result.success(userRepository.save(user));
  }

  public interface Env {
    int BCRYPT_LOG_ROUNDS();
  }

  /** Should rename to Error? */
  public sealed interface UpdateUserByIdError {
    record NotFound(UserNotFoundError error) implements UpdateUserById.UpdateUserByIdError {}

    record EmptyCommand() implements UpdateUserById.UpdateUserByIdError {}

    record ValidationFailed(ValidationNotification<UserDomainError> notification)
        implements UpdateUserById.UpdateUserByIdError {}
  }
}
