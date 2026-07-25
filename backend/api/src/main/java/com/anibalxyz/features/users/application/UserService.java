package com.anibalxyz.features.users.application;

import com.anibalxyz.core.Result;
import com.anibalxyz.core.application.ValidationNotification;
import com.anibalxyz.core.domain.error.DomainError;
import com.anibalxyz.features.users.application.env.UsersEnvironment;
import com.anibalxyz.features.users.application.in.CreateUserCommand;
import com.anibalxyz.features.users.application.in.UpdateUserCommand;
import com.anibalxyz.features.users.domain.*;
import com.anibalxyz.features.users.domain.error.*;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UserService {
  private static final Logger log = LoggerFactory.getLogger(UserService.class);
  private final UsersEnvironment env;
  private final UserRepository userRepository;

  public UserService(UsersEnvironment env, UserRepository userRepository) {
    this.env = env;
    this.userRepository = userRepository;
  }

  public List<User> getAllUsers() {
    return userRepository.findAll();
  }

  public Result<User, UserNotFoundError> getUserById(int id) {
    return userRepository
        .findById(id)
        .<Result<User, UserNotFoundError>>map(Result::success)
        .orElseGet(() -> Result.failure(UserNotFoundError.byId(id)));
  }

  // TODO: make it a sealed interface
  //       I found it difficult to use, e.g. during tests. Consumer is not able to know the
  //       available errors just by reading the method signature -> it must read the method
  public Result<User, DomainError> getUserByEmail(String email) {
    return Email.of(email)
        .<DomainError>mapError(err -> err)
        .flatMap(
            validEmail ->
                userRepository
                    .findByEmail(validEmail)
                    .map(Result::<User, DomainError>success)
                    .orElseGet(() -> Result.failure(UserNotFoundError.byEmail(email))));
  }

  public Result<User, ValidationNotification<UserDomainError>> createUser(
      CreateUserCommand command) {
    ValidationNotification<UserDomainError> notification = new ValidationNotification<>();

    Result<Name, InvalidNameError> nameResult =
        Name.of(command.name()).onFailure(err -> notification.add("name", err));

    Result<Email, InvalidEmailError> emailResult =
        Email.of(command.email())
            .onFailure(err -> notification.add("email", err))
            .onSuccess(
                validEmail ->
                    userRepository
                        .findByEmail(validEmail)
                        .ifPresent(
                            user -> notification.add("email", new EmailAlreadyTakenError())));

    Result<PasswordHash, InvalidPasswordError> passwordResult =
        PasswordHash.generate(command.password(), env.BCRYPT_LOG_ROUNDS())
            .onFailure(err -> notification.add("password", err));

    if (notification.hasErrors()) {
      return Result.failure(notification);
    }

    log.info("User created");
    return Result.success(
        userRepository.save(
            new User(nameResult.unwrap(), emailResult.unwrap(), passwordResult.unwrap())));
  }

  public Result<User, UpdateUserByIdError> updateUserById(Integer id, UpdateUserCommand command) {
    // Fail-fast: avoids unnecessary expensive operations (e.g., bcrypt) when all fields are absent.
    // Redundant with VO null checks, but the performance trade-off justifies it.
    if (!command.hasAtLeastOneField()) {
      return Result.failure(new UpdateUserByIdError.EmptyCommand());
    }

    // TODO: I/O operation should be the last one
    Optional<User> userOptional = userRepository.findById(id);
    if (userOptional.isEmpty()) {
      return Result.failure(new UpdateUserByIdError.NotFound(UserNotFoundError.byId(id)));
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
        if (email.equals(user.email())) break;

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
      return Result.failure(new UpdateUserByIdError.ValidationFailed(notification));
    }

    log.info("User updated");
    return Result.success(userRepository.save(user));
  }

  /**
   * Deletes a user by their ID.
   *
   * @param id The ID of the user to delete.
   */
  public Result<Void, UserNotFoundError> deleteUserById(int id) {
    if (userRepository.deleteById(id)) {
      log.info("User deleted");
      return Result.success();
    }
    return Result.failure(UserNotFoundError.byId(id));
  }

  public sealed interface UpdateUserByIdError {
    record NotFound(UserNotFoundError error) implements UpdateUserByIdError {}

    record EmptyCommand() implements UpdateUserByIdError {}

    record ValidationFailed(ValidationNotification<UserDomainError> notification)
        implements UpdateUserByIdError {}
  }
}
