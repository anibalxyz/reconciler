package com.anibalxyz.features.users.application;

import static com.anibalxyz.shared.Constants.Auth.MINIMUM_BCRYPT_LOG_ROUNDS;
import static com.anibalxyz.shared.Constants.Users.*;
import static com.anibalxyz.shared.NotificationAssert.assertThatNotification;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.when;

import com.anibalxyz.features.users.api.in.CreateUserRequest;
import com.anibalxyz.features.users.application.in.CreateUserCommand;
import com.anibalxyz.features.users.domain.*;
import com.anibalxyz.features.users.domain.error.*;
import com.anibalxyz.shared.ResultAsserts;
import com.anibalxyz.shared.UnitTest;
import java.util.Optional;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;

@DisplayName("Tests for CreateUser service")
public class CreateUserTest extends UnitTest {
  @Mock private UserRepository userRepository;

  private CreateUser createUser;

  @BeforeEach
  void deps() {
    CreateUser.Env env = new TestEnv(MINIMUM_BCRYPT_LOG_ROUNDS);
    createUser = new CreateUser(env, userRepository);
  }

  @Test
  @DisplayName("execute: given a missing name, then return Absent error on name")
  public void execute_missingName_returnAbsentErrorOnName() {
    CreateUserCommand command =
        new CreateUserCommand(null, "mail@email.com", VALID_PASSWORD_STRING);
    when(userRepository.findByEmail(ResultAsserts.success(Email.of(command.email()))))
        .thenReturn(Optional.empty());

    var result = createUser.execute(command);

    var failure = ResultAsserts.failure(result);
    assertThat(failure.getErrors())
        .satisfiesExactly(
            e -> {
              assertThat(e.field()).isEqualTo("name");
              assertThat(e.error())
                  .isInstanceOf(InvalidNameError.class)
                  .extracting(err -> ((InvalidNameError) err).getReason())
                  .isInstanceOf(InvalidNameError.Reason.Absent.class);
            });
  }

  @Test
  @DisplayName("execute: given a blank name, then return Blank error on name")
  public void execute_blankName_returnBlankErrorOnName() {
    CreateUserCommand command = new CreateUserCommand(" ", "mail@email.com", VALID_PASSWORD_STRING);
    when(userRepository.findByEmail(ResultAsserts.success(Email.of(command.email()))))
        .thenReturn(Optional.empty());

    var result = createUser.execute(command);

    var failure = ResultAsserts.failure(result);
    assertThat(failure.getErrors())
        .satisfiesExactly(
            e -> {
              assertThat(e.field()).isEqualTo("name");
              assertThat(e.error())
                  .isInstanceOf(InvalidNameError.class)
                  .extracting(err -> ((InvalidNameError) err).getReason())
                  .isInstanceOf(InvalidNameError.Reason.Blank.class);
            });
  }

  @Test
  @DisplayName("execute: given a missing email, then return Absent error on email")
  public void execute_missingEmail_returnAbsentErrorOnEmail() {
    CreateUserCommand command = new CreateUserCommand("User", null, VALID_PASSWORD_STRING);

    var result = createUser.execute(command);

    var failure = ResultAsserts.failure(result);
    assertThat(failure.getErrors())
        .satisfiesExactly(
            e -> {
              assertThat(e.field()).isEqualTo("email");
              assertThat(e.error())
                  .isInstanceOf(InvalidEmailError.class)
                  .extracting(err -> ((InvalidEmailError) err).getReason())
                  .isInstanceOf(InvalidEmailError.Reason.Absent.class);
            });
  }

  @Test
  @DisplayName("execute: given a blank email, then return Blank error on email")
  public void execute_blankEmail_returnBlankErrorOnEmail() {
    CreateUserCommand command = new CreateUserCommand("User", " ", VALID_PASSWORD_STRING);

    var result = createUser.execute(command);

    var failure = ResultAsserts.failure(result);
    assertThat(failure.getErrors())
        .satisfiesExactly(
            e -> {
              assertThat(e.field()).isEqualTo("email");
              assertThat(e.error())
                  .isInstanceOf(InvalidEmailError.class)
                  .extracting(err -> ((InvalidEmailError) err).getReason())
                  .isInstanceOf(InvalidEmailError.Reason.Blank.class);
            });
  }

  @Test
  @DisplayName("execute: given a missing password, then return Absent error on password")
  public void execute_missingPassword_returnAbsentErrorOnPassword() {
    CreateUserCommand command = new CreateUserCommand("User", "mail@email.com", null);
    when(userRepository.findByEmail(ResultAsserts.success(Email.of(command.email()))))
        .thenReturn(Optional.empty());

    var result = createUser.execute(command);

    var failure = ResultAsserts.failure(result);
    assertThat(failure.getErrors())
        .satisfiesExactly(
            e -> {
              assertThat(e.field()).isEqualTo("password");
              assertThat(e.error())
                  .isInstanceOf(InvalidPasswordError.class)
                  .extracting(err -> ((InvalidPasswordError) err).getReason())
                  .isInstanceOf(InvalidPasswordError.Reason.Absent.class);
            });
  }

  @Test
  @DisplayName("execute: given a blank password, then return Blank error on password")
  public void execute_blankPassword_returnBlankErrorOnPassword() {
    CreateUserCommand command = new CreateUserCommand("User", "mail@email.com", " ");
    when(userRepository.findByEmail(ResultAsserts.success(Email.of(command.email()))))
        .thenReturn(Optional.empty());

    var result = createUser.execute(command);

    var failure = ResultAsserts.failure(result);
    assertThat(failure.getErrors())
        .satisfiesExactly(
            e -> {
              assertThat(e.field()).isEqualTo("password");
              assertThat(e.error())
                  .isInstanceOf(InvalidPasswordError.class)
                  .extracting(err -> ((InvalidPasswordError) err).getReason())
                  .isInstanceOf(InvalidPasswordError.Reason.Blank.class);
            });
  }

  @Test
  @DisplayName("execute: given an existing email, then return EmailAlreadyTakenError on email")
  public void execute_existingEmail_returnEmailAlreadyTakenError() {
    CreateUserCommand command =
        new CreateUserCommand("User 1", VALID_EMAIL_STRING, VALID_PASSWORD_STRING);
    User existing = buildUser(1, command.email());
    when(userRepository.findByEmail(ResultAsserts.success(Email.of(command.email()))))
        .thenReturn(Optional.of(existing));

    var result = createUser.execute(command);

    var failure = ResultAsserts.failure(result);
    assertThatNotification(failure).hasErrorOn("email", EmailAlreadyTakenError.class);
  }

  @ParameterizedTest
  @ValueSource(strings = {"some@mail", "some-mail.com", "@mail.com"})
  @DisplayName("execute: given an invalid email format, then return InvalidFormat error on email")
  public void execute_invalidEmail_returnInvalidFormatErrorOnEmail(String email) {
    CreateUserCommand command = new CreateUserCommand("User", email, VALID_PASSWORD_STRING);

    var result = createUser.execute(command);

    var failure = ResultAsserts.failure(result);
    assertThat(failure.getErrors())
        .satisfiesExactly(
            e -> {
              assertThat(e.field()).isEqualTo("email");
              assertThat(e.error()).isInstanceOf(InvalidEmailError.class);
            });
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "short",
        "long-72-chars-12345678901234567890123456789012345678901234567890123456789"
      })
  @DisplayName(
      "execute: given an invalid password value, then return InvalidValue error on password")
  public void execute_invalidPassword_returnInvalidValueErrorOnPassword(String password) {
    CreateUserCommand command = new CreateUserCommand("User", "mail@email.com", password);
    when(userRepository.findByEmail(ResultAsserts.success(Email.of(command.email()))))
        .thenReturn(Optional.empty());

    var result = createUser.execute(command);

    var failure = ResultAsserts.failure(result);
    assertThat(failure.getErrors())
        .satisfiesExactly(
            e -> {
              assertThat(e.field()).isEqualTo("password");
              assertThat(e.error()).isInstanceOf(InvalidPasswordError.class);
            });
  }

  @Test
  @DisplayName("execute: given valid data, then return the created user")
  public void execute_validData_returnCreatedUser() {
    CreateUserRequest request =
        new CreateUserRequest("User 1", "user1@mail.com", VALID_PASSWORD_STRING);

    when(userRepository.findByEmail(ResultAsserts.success(Email.of(request.email()))))
        .thenReturn(Optional.empty());
    when(userRepository.save(
            argThat(
                u ->
                    u.name().equals(ResultAsserts.success(Name.of(request.name())))
                        && u.email().equals(ResultAsserts.success(Email.of(request.email())))
                        && u.passwordMatches(request.password()))))
        .thenAnswer(inv -> inv.getArgument(0));

    var result = createUser.execute(request.toCommand());

    User actual = ResultAsserts.success(result);
    assertThat(actual.name()).isEqualTo(ResultAsserts.success(Name.of(request.name())));
    assertThat(actual.email()).isEqualTo(ResultAsserts.success(Email.of(request.email())));
    assertTrue(actual.passwordMatches(request.password()));
  }

  private record TestEnv(int BCRYPT_LOG_ROUNDS) implements CreateUser.Env {}
}
