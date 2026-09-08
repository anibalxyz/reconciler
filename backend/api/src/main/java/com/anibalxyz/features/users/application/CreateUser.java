package com.anibalxyz.features.users.application;

import com.anibalxyz.core.application.ValidationNotification;
import com.anibalxyz.core.primitives.Result;
import com.anibalxyz.features.users.application.in.CreateUserCommand;
import com.anibalxyz.features.users.domain.*;
import com.anibalxyz.features.users.domain.error.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CreateUser {
  private static final Logger log = LoggerFactory.getLogger(CreateUser.class);

  private final Settings settings;
  private final UserRepository userRepository;

  public CreateUser(Settings settings, UserRepository userRepository) {
    this.settings = settings;
    this.userRepository = userRepository;
  }

  public Result<User, ValidationNotification<UserDomainError>> execute(CreateUserCommand command) {
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

    Result<Password, InvalidPasswordError> passwordResult =
        Password.of(command.password()).onFailure(err -> notification.add("password", err));

    if (notification.hasErrors()) {
      return Result.failure(notification);
    }

    PasswordHash password = PasswordHash.of(passwordResult.unwrap(), settings.bcryptLogRounds());

    log.info("User created");
    return Result.success(
        userRepository.save(User.create(nameResult.unwrap(), emailResult.unwrap(), password)));
  }

  public interface Settings {
    int bcryptLogRounds();
  }
}
