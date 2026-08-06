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

  public Result<User, Error> execute(Integer id, UpdateUserCommand command) {
    ValidationNotification<UserDomainError> notification = new ValidationNotification<>();
    UpdatingData data = validateData(command, notification);

    if (allFieldsAreEmpty(data, notification)) {
      return Result.failure(new Error.EmptyCommand());
    }

    Optional<User> userOptional = userRepository.findById(id);
    if (userOptional.isEmpty()) {
      return Result.failure(new Error.NotFound(UserNotFoundError.byId(id)));
    }
    User existingUser = userOptional.get();

    data.email().ifPresent(email -> ensureEmailIsNotTaken(email, existingUser, notification));

    if (notification.hasErrors()) {
      return Result.failure(new Error.ValidationFailed(notification));
    }

    User updatedUser = assignUserData(existingUser, data);

    log.info("User updated");
    return Result.success(userRepository.save(updatedUser));
  }

  /**
   * Checks all fields are empty through the following steps/filters:
   *
   * <p>{@code data} is empty when every field failed validation (reason unknown yet: absent,
   * tooLong, format...).
   *
   * <p>The {@code notification} must also be error-free, since errors are only stored for
   * non-absent reasons.
   *
   * @return {@code true} if both {@code data} and {@code notification} are empty.
   */
  private static boolean allFieldsAreEmpty(
      UpdatingData data, ValidationNotification<UserDomainError> notification) {
    return data.name().isEmpty()
        && data.email().isEmpty()
        && data.password().isEmpty()
        && !notification.hasErrors();
  }

  private static UpdatingData validateData(
      UpdateUserCommand command, ValidationNotification<UserDomainError> notification) {
    UpdatingData data = UpdatingData.init();

    switch (Name.of(command.name())) {
      case Result.Failure(var err) -> {
        if (!(err.getReason() instanceof InvalidNameError.Reason.Absent)) {
          notification.add("name", err);
        }
      }
      case Result.Success(var name) -> data = data.withName(name);
    }

    switch (Email.of(command.email())) {
      case Result.Failure(var err) -> {
        if (!(err.getReason() instanceof InvalidEmailError.Reason.Absent)) {
          notification.add("email", err);
        }
      }
      case Result.Success(var email) -> data = data.withEmail(email);
    }

    switch (Password.of(command.password())) {
      case Result.Failure(var err) -> {
        if (!(err.getReason() instanceof InvalidPasswordError.Reason.Absent)) {
          notification.add("password", err);
        }
      }
      case Result.Success(var password) -> data = data.withPassword(password);
    }
    return data;
  }

  private User assignUserData(User user, UpdatingData data) {
    user = data.name().map(user::withName).orElse(user);
    user = data.email().map(user::withEmail).orElse(user);
    user =
        data.password()
            .map(password -> PasswordHash.of(password, env.BCRYPT_LOG_ROUNDS()))
            .map(user::withPasswordHash)
            .orElse(user);
    return user;
  }

  private void ensureEmailIsNotTaken(
      Email email, User user, ValidationNotification<UserDomainError> notification) {

    boolean emailAlreadyFromUser = user.email().equals(email);
    if (!emailAlreadyFromUser && userRepository.findByEmail(email).isPresent()) {
      notification.add("email", new EmailAlreadyTakenError());
    }
  }

  public interface Env {
    int BCRYPT_LOG_ROUNDS();
  }

  public sealed interface Error {
    record NotFound(UserNotFoundError error) implements Error {}

    record EmptyCommand() implements Error {}

    record ValidationFailed(ValidationNotification<UserDomainError> notification)
        implements Error {}
  }

  private record UpdatingData(
      Optional<Name> name, Optional<Email> email, Optional<Password> password) {
    public static UpdatingData init() {
      return new UpdatingData(Optional.empty(), Optional.empty(), Optional.empty());
    }

    public UpdatingData withName(Name name) {
      return new UpdatingData(Optional.of(name), email, password);
    }

    public UpdatingData withEmail(Email email) {
      return new UpdatingData(name, Optional.of(email), password);
    }

    public UpdatingData withPassword(Password password) {
      return new UpdatingData(name, email, Optional.of(password));
    }
  }
}
