package com.anibalxyz.features.users.application;

import static com.anibalxyz.shared.Constants.Users.VALID_PASSWORD;
import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.when;

import com.anibalxyz.core.application.ValidationNotification;
import com.anibalxyz.core.domain.error.DomainError;
import com.anibalxyz.core.domain.error.ReasonedError;
import com.anibalxyz.features.users.api.in.UserCreateRequest;
import com.anibalxyz.features.users.application.in.CreateUserCommand;
import com.anibalxyz.features.users.application.in.UpdateUserCommand;
import com.anibalxyz.features.users.domain.*;
import com.anibalxyz.features.users.domain.error.*;
import com.anibalxyz.shared.Constants;
import com.anibalxyz.shared.ResultAsserts;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

// TODO: cache or pre-create basic/common objects to speed up tests
//       e.g. creating a PasswordHash object is expensive.

@ExtendWith(MockitoExtension.class)
@DisplayName("Tests for UserService")
public class UserServiceTest {

  public static int BCRYPT_LOG_ROUNDS;

  @Mock private UserRepository userRepository;

  private UserService userService;

  @BeforeAll
  public static void setup() {
    Constants.init();
    BCRYPT_LOG_ROUNDS = Constants.APP_ENV.BCRYPT_LOG_ROUNDS();
  }

  private static User buildUser(int id, String email) {
    return new User(
        id,
        ResultAsserts.success(Name.of("User")),
        ResultAsserts.success(Email.of(email)),
        ResultAsserts.success(PasswordHash.generate(VALID_PASSWORD, BCRYPT_LOG_ROUNDS)),
        Instant.now(),
        Instant.now());
  }

  private static void assertHasFailureOn(
      ValidationNotification<? extends DomainError> n,
      String field,
      Class<? extends DomainError> errorClass) {
    assertThat(n.getErrors())
        .satisfiesExactly(
            e -> {
              assertThat(e.field()).isEqualTo(field);
              assertThat(e.error()).isInstanceOf(errorClass);
            });
  }

  @BeforeEach
  public void di() {
    userService = new UserService(Constants.APP_CONFIG.env(), userRepository);
  }

  @Nested
  @DisplayName("Success Scenarios")
  class SuccessScenarios {

    @Test
    @DisplayName("getAllUsers: given users exist, then return a list of all users")
    public void getAllUsers_usersExist_returnListOfUsers() {
      List<User> expectedUsers =
          List.of(buildUser(1, "user1@mail.com"), buildUser(2, "user2@mail.com"));
      when(userRepository.findAll()).thenReturn(expectedUsers);

      assertThat(userService.getAllUsers()).isEqualTo(expectedUsers);
    }

    @Test
    @DisplayName("getAllUsers: given no users exist, then return an empty list")
    public void getAllUsers_noUsersExist_returnEmptyList() {
      when(userRepository.findAll()).thenReturn(List.of());

      assertThat(userService.getAllUsers()).isEmpty();
    }

    @Test
    @DisplayName("getUserById: given an existing id, then return the correct user")
    public void getUserById_existingId_returnUser() {
      User expected = buildUser(1, "user1@mail.com");
      when(userRepository.findById(expected.id())).thenReturn(Optional.of(expected));

      var result = userService.getUserById(expected.id());

      User actual = ResultAsserts.success(result);
      assertThat(actual).isEqualTo(expected);
    }

    @Test
    @DisplayName("getUserByEmail: given an existing email, then return the correct user")
    public void getUserByEmail_existingEmail_returnUser() {
      User expected = buildUser(1, "user1@mail.com");
      when(userRepository.findByEmail(expected.email())).thenReturn(Optional.of(expected));

      var result = userService.getUserByEmail(expected.email().value());

      User actual = ResultAsserts.success(result);
      assertThat(actual).isEqualTo(expected);
    }

    @Test
    @DisplayName("createUser: given valid data, then return the created user")
    public void createUser_validData_returnCreatedUser() {
      UserCreateRequest request = new UserCreateRequest("User 1", "user1@mail.com", VALID_PASSWORD);

      when(userRepository.findByEmail(ResultAsserts.success(Email.of(request.email()))))
          .thenReturn(Optional.empty());
      when(userRepository.save(
              argThat(
                  u ->
                      u.name().equals(ResultAsserts.success(Name.of(request.name())))
                          && u.email().equals(ResultAsserts.success(Email.of(request.email())))
                          && u.passwordHash().matches(request.password()))))
          .thenAnswer(inv -> inv.getArgument(0));

      var result = userService.createUser(request.toCommand());

      User actual = ResultAsserts.success(result);
      assertThat(actual.name()).isEqualTo(ResultAsserts.success(Name.of(request.name())));
      assertThat(actual.email()).isEqualTo(ResultAsserts.success(Email.of(request.email())));
      assertTrue(actual.passwordHash().matches(request.password()));
    }

    @Test
    @DisplayName("updateUserById: given a valid id and name, then return the updated user")
    public void updateUserById_validIdAndName_returnUpdatedUser() {
      User existing = buildUser(1, "previous@mail.com");
      UpdateUserCommand command = new UpdateUserCommand("Valid Name", null, null);
      User expected = existing.withName(ResultAsserts.success(Name.of(command.name())));

      when(userRepository.findById(existing.id())).thenReturn(Optional.of(existing));
      when(userRepository.save(expected)).thenAnswer(inv -> inv.getArgument(0));

      var result = userService.updateUserById(existing.id(), command);

      User actual = ResultAsserts.success(result);
      assertThat(actual).isEqualTo(expected);
    }

    @Test
    @DisplayName("updateUserById: given a valid id and email, then return the updated user")
    public void updateUserById_validIdAndEmail_returnUpdatedUser() {
      User existing = buildUser(1, "previous@mail.com");
      UpdateUserCommand command = new UpdateUserCommand(null, "new@mail.com", null);
      User expected = existing.withEmail(ResultAsserts.success(Email.of(command.email())));

      when(userRepository.findById(existing.id())).thenReturn(Optional.of(existing));
      when(userRepository.findByEmail(expected.email())).thenReturn(Optional.empty());
      when(userRepository.save(expected)).thenAnswer(inv -> inv.getArgument(0));

      var result = userService.updateUserById(existing.id(), command);

      User actual = ResultAsserts.success(result);
      assertThat(actual).isEqualTo(expected);
    }

    @Test
    @DisplayName("updateUserById: given a valid id and password, then return the updated user")
    public void updateUserById_validIdAndPassword_returnUpdatedUser() {
      User existing = buildUser(1, "previous@mail.com");
      String newPassword = "new" + VALID_PASSWORD;
      UpdateUserCommand command = new UpdateUserCommand(null, null, newPassword);

      when(userRepository.findById(existing.id())).thenReturn(Optional.of(existing));
      when(userRepository.save(
              argThat(
                  u ->
                      u.id().equals(existing.id())
                          && u.email().equals(existing.email())
                          && u.passwordHash().matches(newPassword))))
          .thenAnswer(inv -> inv.getArgument(0));

      var result = userService.updateUserById(existing.id(), command);

      User actual = ResultAsserts.success(result);
      assertThat(actual.passwordHash().matches(newPassword)).isTrue();
    }

    @Test
    @DisplayName(
        "updateUserById: given an email already in use by the user, then return the unmodified user")
    public void updateUserById_emailAlreadyUsedByUser_returnUnmodifiedUser() {
      User existing = buildUser(1, "same@mail.com");
      UpdateUserCommand command = new UpdateUserCommand(null, "same@mail.com", null);

      when(userRepository.findById(existing.id())).thenReturn(Optional.of(existing));
      when(userRepository.save(existing)).thenAnswer(inv -> inv.getArgument(0));

      var result = userService.updateUserById(existing.id(), command);

      User user = ResultAsserts.success(result);
      assertThat(user).isEqualTo(existing);
    }

    @Test
    @DisplayName("deleteUserById: given an existing id, then return success")
    public void deleteUserById_existingId_returnSuccess() {
      when(userRepository.deleteById(1)).thenReturn(true);

      var result = userService.deleteUserById(1);

      assertThat(ResultAsserts.success(result)).isNull();
    }
  }

  @Nested
  @DisplayName("Failure Scenarios")
  class FailureScenarios {

    @Test
    @DisplayName("getUserById: given a non-existing id, then return UserNotFoundError")
    public void getUserById_nonExistingId_returnUserNotFoundError() {
      int id = 999;
      when(userRepository.findById(id)).thenReturn(Optional.empty());

      var result = userService.getUserById(id);

      var failure = ResultAsserts.failure(result);
      assertThat(failure)
          .isInstanceOf(UserNotFoundError.class)
          .extracting(ReasonedError::getReason)
          .isEqualTo(new UserNotFoundError.Reason.ById(id));
    }

    @Test
    @DisplayName("getUserByEmail: given a non-existing email, then return UserNotFoundError")
    public void getUserByEmail_nonExistingEmail_returnUserNotFoundError() {
      String email = "non.existing@mail.com";
      when(userRepository.findByEmail(ResultAsserts.success(Email.of(email))))
          .thenReturn(Optional.empty());

      var result = userService.getUserByEmail(email);

      var failure = ResultAsserts.failure(result);
      assertThat(failure)
          .isInstanceOf(UserNotFoundError.class)
          .extracting(e -> ((UserNotFoundError) e).getReason())
          .isEqualTo(new UserNotFoundError.Reason.ByEmail(email));
    }

    @Test
    @DisplayName(
        "getUserByEmail: given a blank email, then return InvalidEmailError with Blank reason")
    public void getUserByEmail_blankEmail_returnInvalidEmailErrorWithBlankReason() {
      var result = userService.getUserByEmail(" ");

      var failure = ResultAsserts.failure(result);
      assertThat(failure)
          .isInstanceOf(InvalidEmailError.class)
          .extracting(e -> ((InvalidEmailError) e).getReason())
          .isInstanceOf(InvalidEmailError.Reason.Blank.class);
    }

    @Test
    @DisplayName(
        "getUserByEmail: given an invalid email format, then return InvalidEmailError with InvalidFormat reason")
    public void getUserByEmail_invalidEmailFormat_returnInvalidEmailErrorWithInvalidFormatReason() {
      var result = userService.getUserByEmail("mailemail.com");

      var err = ResultAsserts.failure(result);
      assertThat(err)
          .isInstanceOf(InvalidEmailError.class)
          .extracting(e -> ((InvalidEmailError) e).getReason())
          .isEqualTo(new InvalidEmailError.Reason.InvalidFormat());
    }

    @Test
    @DisplayName("createUser: given a missing name, then return Absent error on name")
    public void createUser_missingName_returnAbsentErrorOnName() {
      CreateUserCommand command = new CreateUserCommand(null, "mail@email.com", VALID_PASSWORD);
      when(userRepository.findByEmail(ResultAsserts.success(Email.of(command.email()))))
          .thenReturn(Optional.empty());

      var result = userService.createUser(command);

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
    @DisplayName("createUser: given a blank name, then return Blank error on name")
    public void createUser_blankName_returnBlankErrorOnName() {
      CreateUserCommand command = new CreateUserCommand(" ", "mail@email.com", VALID_PASSWORD);
      when(userRepository.findByEmail(ResultAsserts.success(Email.of(command.email()))))
          .thenReturn(Optional.empty());

      var result = userService.createUser(command);

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
    @DisplayName("createUser: given a missing email, then return Absent error on email")
    public void createUser_missingEmail_returnAbsentErrorOnEmail() {
      CreateUserCommand command = new CreateUserCommand("User", null, VALID_PASSWORD);

      var result = userService.createUser(command);

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
    @DisplayName("createUser: given a blank email, then return Blank error on email")
    public void createUser_blankEmail_returnBlankErrorOnEmail() {
      CreateUserCommand command = new CreateUserCommand("User", " ", VALID_PASSWORD);

      var result = userService.createUser(command);

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
    @DisplayName("createUser: given a missing password, then return Absent error on password")
    public void createUser_missingPassword_returnAbsentErrorOnPassword() {
      CreateUserCommand command = new CreateUserCommand("User", "mail@email.com", null);
      when(userRepository.findByEmail(ResultAsserts.success(Email.of(command.email()))))
          .thenReturn(Optional.empty());

      var result = userService.createUser(command);

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
    @DisplayName("createUser: given a blank password, then return Blank error on password")
    public void createUser_blankPassword_returnBlankErrorOnPassword() {
      CreateUserCommand command = new CreateUserCommand("User", "mail@email.com", " ");
      when(userRepository.findByEmail(ResultAsserts.success(Email.of(command.email()))))
          .thenReturn(Optional.empty());

      var result = userService.createUser(command);

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
    @DisplayName("createUser: given an existing email, then return EmailAlreadyTakenError on email")
    public void createUser_existingEmail_returnEmailAlreadyTakenError() {
      CreateUserCommand command = new CreateUserCommand("User 1", "user1@mail.com", VALID_PASSWORD);
      User existing = buildUser(1, command.email());
      when(userRepository.findByEmail(ResultAsserts.success(Email.of(command.email()))))
          .thenReturn(Optional.of(existing));

      var result = userService.createUser(command);

      var failure = ResultAsserts.failure(result);
      assertHasFailureOn(failure, "email", EmailAlreadyTakenError.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {"some@mail", "some-mail.com", "@mail.com"})
    @DisplayName(
        "createUser: given an invalid email format, then return InvalidFormat error on email")
    public void createUser_invalidEmail_returnInvalidFormatErrorOnEmail(String email) {
      CreateUserCommand command = new CreateUserCommand("User", email, VALID_PASSWORD);

      var result = userService.createUser(command);

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
        "createUser: given an invalid password value, then return InvalidValue error on password")
    public void createUser_invalidPassword_returnInvalidValueErrorOnPassword(String password) {
      CreateUserCommand command = new CreateUserCommand("User", "mail@email.com", password);
      when(userRepository.findByEmail(ResultAsserts.success(Email.of(command.email()))))
          .thenReturn(Optional.empty());

      var result = userService.createUser(command);

      var failure = ResultAsserts.failure(result);
      assertThat(failure.getErrors())
          .satisfiesExactly(
              e -> {
                assertThat(e.field()).isEqualTo("password");
                assertThat(e.error()).isInstanceOf(InvalidPasswordError.class);
              });
    }

    @Test
    @DisplayName("updateUserById: given a non-existing id, then return NotFound error")
    public void updateUserById_nonExistingId_returnNotFoundError() {
      int id = 999;
      UpdateUserCommand command = new UpdateUserCommand("New Name", null, null);
      when(userRepository.findById(id)).thenReturn(Optional.empty());

      var result = userService.updateUserById(id, command);

      var failure = ResultAsserts.failure(result);
      assertThat(failure)
          .isInstanceOf(UserService.UpdateUserByIdError.NotFound.class)
          .extracting(e -> ((UserService.UpdateUserByIdError.NotFound) e).error().getReason())
          .isEqualTo(new UserNotFoundError.Reason.ById(id));
    }

    @Test
    @DisplayName("updateUserById: given all fields empty, then return EmptyCommand error")
    public void updateUserById_emptyCommand_returnEmptyCommandFailure() {
      UpdateUserCommand command = new UpdateUserCommand(null, null, null);

      var result = userService.updateUserById(1, command);

      var failure = ResultAsserts.failure(result);
      assertThat(failure).isInstanceOf(UserService.UpdateUserByIdError.EmptyCommand.class);
    }

    @Test
    @DisplayName("updateUserById: given a blank name, then return Blank error on name")
    public void updateUserById_blankName_returnBlankErrorOnName() {
      User existing = buildUser(1, "previous@mail.com");
      UpdateUserCommand command = new UpdateUserCommand(" ", null, VALID_PASSWORD);
      when(userRepository.findById(existing.id())).thenReturn(Optional.of(existing));

      var result = userService.updateUserById(existing.id(), command);

      var failure = ResultAsserts.failure(result);
      assertThat(failure).isInstanceOf(UserService.UpdateUserByIdError.ValidationFailed.class);

      var notification =
          ((UserService.UpdateUserByIdError.ValidationFailed) failure).notification();
      assertThat(notification.getErrors())
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
    @DisplayName("updateUserById: given a blank email, then return Blank error on email")
    public void updateUserById_blankEmail_returnBlankErrorOnEmail() {
      User existing = buildUser(1, "previous@mail.com");
      UpdateUserCommand command = new UpdateUserCommand(null, " ", VALID_PASSWORD);
      when(userRepository.findById(existing.id())).thenReturn(Optional.of(existing));

      var result = userService.updateUserById(existing.id(), command);

      var failure = ResultAsserts.failure(result);
      assertThat(failure).isInstanceOf(UserService.UpdateUserByIdError.ValidationFailed.class);

      var notification =
          ((UserService.UpdateUserByIdError.ValidationFailed) failure).notification();
      assertThat(notification.getErrors())
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
    @DisplayName("updateUserById: given a blank password, then return Blank error on password")
    public void updateUserById_blankPassword_returnBlankErrorOnPassword() {
      User existing = buildUser(1, "previous@mail.com");
      UpdateUserCommand command = new UpdateUserCommand("New Name", null, " ");
      when(userRepository.findById(existing.id())).thenReturn(Optional.of(existing));

      var result = userService.updateUserById(existing.id(), command);

      var failure = ResultAsserts.failure(result);
      assertThat(failure).isInstanceOf(UserService.UpdateUserByIdError.ValidationFailed.class);

      var notification =
          ((UserService.UpdateUserByIdError.ValidationFailed) failure).notification();
      assertThat(notification.getErrors())
          .satisfiesExactly(
              e -> {
                assertThat(e.field()).isEqualTo("password");
                assertThat(e.error())
                    .isInstanceOf(InvalidPasswordError.class)
                    .extracting(err -> ((InvalidPasswordError) err).getReason())
                    .isInstanceOf(InvalidPasswordError.Reason.Blank.class);
              });
    }

    @ParameterizedTest
    @ValueSource(strings = {"some@mail", "some-mail.com", "@mail.com"})
    @DisplayName(
        "updateUserById: given an invalid email format, then return InvalidFormat error on email")
    public void updateUserById_invalidEmail_returnInvalidFormatErrorOnEmail(String email) {
      User existing = buildUser(1, "previous@mail.com");
      UpdateUserCommand command = new UpdateUserCommand(null, email, null);
      when(userRepository.findById(existing.id())).thenReturn(Optional.of(existing));

      var result = userService.updateUserById(existing.id(), command);

      var failure = ResultAsserts.failure(result);
      assertThat(failure).isInstanceOf(UserService.UpdateUserByIdError.ValidationFailed.class);

      var notification =
          ((UserService.UpdateUserByIdError.ValidationFailed) failure).notification();
      assertThat(notification.getErrors())
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
        "updateUserById: given an invalid password value, then return InvalidValue error on password")
    public void updateUserById_invalidPassword_returnInvalidValueErrorOnPassword(String password) {
      User existing = buildUser(1, "previous@mail.com");
      UpdateUserCommand command = new UpdateUserCommand(null, null, password);
      when(userRepository.findById(existing.id())).thenReturn(Optional.of(existing));

      var result = userService.updateUserById(existing.id(), command);

      var failure = ResultAsserts.failure(result);
      assertThat(failure).isInstanceOf(UserService.UpdateUserByIdError.ValidationFailed.class);

      var notification =
          ((UserService.UpdateUserByIdError.ValidationFailed) failure).notification();
      assertThat(notification.getErrors())
          .satisfiesExactly(
              e -> {
                assertThat(e.field()).isEqualTo("password");
                assertThat(e.error()).isInstanceOf(InvalidPasswordError.class);
              });
    }

    @Test
    @DisplayName(
        "updateUserById: given an email already in use, then return EmailAlreadyTakenError on email")
    public void updateUserById_emailAlreadyInUse_returnEmailAlreadyTakenError() {
      User existing = buildUser(2, "previous@mail.com");
      UpdateUserCommand command = new UpdateUserCommand(null, "taken@mail.com", null);
      when(userRepository.findById(1)).thenReturn(Optional.of(existing));
      when(userRepository.findByEmail(ResultAsserts.success(Email.of(command.email()))))
          .thenReturn(Optional.of(existing));

      var result = userService.updateUserById(1, command);

      var failure = ResultAsserts.failure(result);
      assertThat(failure).isInstanceOf(UserService.UpdateUserByIdError.ValidationFailed.class);

      var notification =
          ((UserService.UpdateUserByIdError.ValidationFailed) failure).notification();
      assertHasFailureOn(notification, "email", EmailAlreadyTakenError.class);
    }

    @Test
    @DisplayName("deleteUserById: given a non-existing id, then return UserNotFoundError")
    public void deleteUserById_nonExistingId_returnUserNotFoundError() {
      int id = 999;
      when(userRepository.deleteById(id)).thenReturn(false);

      var result = userService.deleteUserById(id);

      var failure = ResultAsserts.failure(result);
      assertThat(failure)
          .isInstanceOf(UserNotFoundError.class)
          .extracting(ReasonedError::getReason)
          .isEqualTo(new UserNotFoundError.Reason.ById(id));
    }
  }
}
