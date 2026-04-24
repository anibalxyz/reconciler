package com.anibalxyz.features.users.application;

import static com.anibalxyz.shared.Constants.Users.VALID_PASSWORD;
import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.when;

import com.anibalxyz.features.common.Result;
import com.anibalxyz.features.common.application.ValidationNotification;
import com.anibalxyz.features.common.domain.error.DomainError;
import com.anibalxyz.features.common.domain.error.ReasonedError;
import com.anibalxyz.features.users.api.in.UserCreateRequest;
import com.anibalxyz.features.users.api.in.UserUpdateRequest;
import com.anibalxyz.features.users.application.in.UpdateUserCommand;
import com.anibalxyz.features.users.domain.*;
import com.anibalxyz.features.users.domain.error.*;
import com.anibalxyz.shared.Constants;
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
        Name.of("User").getValue(),
        Email.of(email).getValue(),
        PasswordHash.generate(VALID_PASSWORD, BCRYPT_LOG_ROUNDS).getValue(),
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
    // TODO: env vars should be mocked
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

      Result<User, UserNotFoundError> result = userService.getUserById(expected.id());

      assertThat(result.isSuccess()).isTrue();
      assertThat(result.getValue()).isEqualTo(expected);
    }

    @Test
    @DisplayName("getUserByEmail: given an existing email, then return the correct user")
    public void getUserByEmail_existingEmail_returnUser() {
      User expected = buildUser(1, "user1@mail.com");
      when(userRepository.findByEmail(expected.email())).thenReturn(Optional.of(expected));

      Result<User, DomainError> result = userService.getUserByEmail(expected.email().value());

      assertThat(result.isSuccess()).isTrue();
      assertThat(result.getValue()).isEqualTo(expected);
    }

    @Test
    @DisplayName("createUser: given valid data, then return the created user")
    public void createUser_validData_returnCreatedUser() {
      UserCreateRequest request = new UserCreateRequest("User 1", "user1@mail.com", VALID_PASSWORD);

      when(userRepository.findByEmail(Email.of(request.email()).getValue()))
          .thenReturn(Optional.empty());
      when(userRepository.save(
              argThat(
                  u ->
                      u.name().equals(Name.of(request.name()).getValue())
                          && u.email().equals(Email.of(request.email()).getValue())
                          && u.passwordHash().matches(request.password()))))
          .thenAnswer(inv -> inv.getArgument(0));

      Result<User, ValidationNotification<UserDomainError>> result =
          userService.createUser(request.toCommand());

      assertThat(result.isSuccess()).isTrue();
      User actual = result.getValue();
      assertThat(actual.name()).isEqualTo(Name.of(request.name()).getValue());
      assertThat(actual.email()).isEqualTo(Email.of(request.email()).getValue());
      assertTrue(actual.passwordHash().matches(request.password()));
    }

    @Test
    @DisplayName("updateUserById: given a valid id and name, then return the updated user")
    public void updateUserById_validIdAndName_returnUpdatedUser() {
      User existing = buildUser(1, "previous@mail.com");
      UserUpdateRequest request = new UserUpdateRequest("New Name", null, null);
      User expected = existing.withName(Name.of(request.name()).getValue());

      when(userRepository.findById(existing.id())).thenReturn(Optional.of(existing));
      when(userRepository.save(expected)).thenAnswer(inv -> inv.getArgument(0));

      Result<User, UserService.UpdateUserByIdError> result =
          userService.updateUserById(existing.id(), request.toCommand());

      assertThat(result.isSuccess()).isTrue();
      assertThat(result.getValue()).isEqualTo(expected);
    }

    @Test
    @DisplayName("updateUserById: given a valid id and email, then return the updated user")
    public void updateUserById_validIdAndEmail_returnUpdatedUser() {
      User existing = buildUser(1, "previous@mail.com");
      UserUpdateRequest request = new UserUpdateRequest(null, "new@mail.com", null);
      User expected = existing.withEmail(Email.of(request.email()).getValue());

      when(userRepository.findById(existing.id())).thenReturn(Optional.of(existing));
      when(userRepository.findByEmail(expected.email())).thenReturn(Optional.empty());
      when(userRepository.save(expected)).thenAnswer(inv -> inv.getArgument(0));

      Result<User, UserService.UpdateUserByIdError> result =
          userService.updateUserById(existing.id(), request.toCommand());

      assertThat(result.isSuccess()).isTrue();
      assertThat(result.getValue()).isEqualTo(expected);
    }

    @Test
    @DisplayName("updateUserById: given a valid id and password, then return the updated user")
    public void updateUserById_validIdAndPassword_returnUpdatedUser() {
      User existing = buildUser(1, "previous@mail.com");
      String newPassword = "new" + VALID_PASSWORD;
      UserUpdateRequest request = new UserUpdateRequest(null, null, newPassword);

      when(userRepository.findById(existing.id())).thenReturn(Optional.of(existing));
      when(userRepository.save(
              argThat(
                  u ->
                      u.id().equals(existing.id())
                          && u.email().equals(existing.email())
                          && u.passwordHash().matches(newPassword))))
          .thenAnswer(inv -> inv.getArgument(0));

      Result<User, UserService.UpdateUserByIdError> result =
          userService.updateUserById(existing.id(), request.toCommand());

      assertThat(result.isSuccess()).isTrue();
      assertTrue(result.getValue().passwordHash().matches(newPassword));
    }

    @Test
    @DisplayName(
        "updateUserById: given an email already in use by the user, then return the unmodified user")
    public void updateUserById_emailAlreadyUsedByUser_returnUnmodifiedUser() {
      User existing = buildUser(1, "same@mail.com");
      UserUpdateRequest request = new UserUpdateRequest(null, "same@mail.com", null);

      when(userRepository.findById(existing.id())).thenReturn(Optional.of(existing));
      when(userRepository.save(existing)).thenAnswer(inv -> inv.getArgument(0));

      Result<User, UserService.UpdateUserByIdError> result =
          userService.updateUserById(existing.id(), request.toCommand());

      assertThat(result.isSuccess()).isTrue();
      assertThat(result.getValue()).isEqualTo(existing);
    }

    @Test
    @DisplayName("deleteUserById: given an existing id, then return success")
    public void deleteUserById_existingId_returnSuccess() {
      when(userRepository.deleteById(1)).thenReturn(true);

      Result<Void, UserNotFoundError> result = userService.deleteUserById(1);

      assertThat(result.isSuccess()).isTrue();
      assertThat(result.getValue()).isNull();
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

      Result<User, UserNotFoundError> result = userService.getUserById(id);

      assertThat(result.isFailure()).isTrue();
      assertThat(result.getError())
          .isInstanceOf(UserNotFoundError.class)
          .extracting(ReasonedError::getReason)
          .isEqualTo(new UserNotFoundError.Reason.ById(id));
    }

    @Test
    @DisplayName("getUserByEmail: given a non-existing email, then return UserNotFoundError")
    public void getUserByEmail_nonExistingEmail_returnUserNotFoundError() {
      String email = "non.existing@mail.com";
      when(userRepository.findByEmail(Email.of(email).getValue())).thenReturn(Optional.empty());

      Result<User, DomainError> result = userService.getUserByEmail(email);

      assertThat(result.isFailure()).isTrue();
      assertThat(result.getError())
          .isInstanceOf(UserNotFoundError.class)
          .extracting(e -> ((UserNotFoundError) e).getReason())
          .isEqualTo(new UserNotFoundError.Reason.ByEmail(email));
    }

    @Test
    @DisplayName(
        "getUserByEmail: given a blank email, then return InvalidEmailError with Blank reason")
    public void getUserByEmail_blankEmail_returnInvalidEmailErrorWithBlankReason() {
      Result<User, DomainError> result = userService.getUserByEmail(" ");

      assertThat(result.isFailure()).isTrue();
      assertThat(result.getError())
          .isInstanceOf(InvalidEmailError.class)
          .extracting(e -> ((InvalidEmailError) e).getReason())
          .isInstanceOf(InvalidEmailError.Reason.Blank.class);
    }

    @Test
    @DisplayName(
        "getUserByEmail: given an invalid email format, then return InvalidEmailError with InvalidFormat reason")
    public void getUserByEmail_invalidEmailFormat_returnInvalidEmailErrorWithInvalidFormatReason() {
      Result<User, DomainError> result = userService.getUserByEmail("mailemail.com");

      assertThat(result.isFailure()).isTrue();
      assertThat(result.getError())
          .isInstanceOf(InvalidEmailError.class)
          .extracting(e -> ((InvalidEmailError) e).getReason())
          .isEqualTo(new InvalidEmailError.Reason.InvalidFormat());
    }

    // ── createUser ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("createUser: given a missing name, then return Absent error on name")
    public void createUser_missingName_returnAbsentErrorOnName() {
      UserCreateRequest request = new UserCreateRequest(null, "mail@email.com", VALID_PASSWORD);
      when(userRepository.findByEmail(Email.of(request.email()).getValue()))
          .thenReturn(Optional.empty());

      Result<User, ValidationNotification<UserDomainError>> result =
          userService.createUser(request.toCommand());

      assertThat(result.isFailure()).isTrue();
      assertThat(result.getError().getErrors())
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
      UserCreateRequest request = new UserCreateRequest(" ", "mail@email.com", VALID_PASSWORD);
      when(userRepository.findByEmail(Email.of(request.email()).getValue()))
          .thenReturn(Optional.empty());

      Result<User, ValidationNotification<UserDomainError>> result =
          userService.createUser(request.toCommand());

      assertThat(result.isFailure()).isTrue();
      assertThat(result.getError().getErrors())
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
      UserCreateRequest request = new UserCreateRequest("User", null, VALID_PASSWORD);

      Result<User, ValidationNotification<UserDomainError>> result =
          userService.createUser(request.toCommand());

      assertThat(result.isFailure()).isTrue();
      assertThat(result.getError().getErrors())
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
      UserCreateRequest request = new UserCreateRequest("User", " ", VALID_PASSWORD);

      Result<User, ValidationNotification<UserDomainError>> result =
          userService.createUser(request.toCommand());

      assertThat(result.isFailure()).isTrue();
      assertThat(result.getError().getErrors())
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
      UserCreateRequest request = new UserCreateRequest("User", "mail@email.com", null);
      when(userRepository.findByEmail(Email.of(request.email()).getValue()))
          .thenReturn(Optional.empty());

      Result<User, ValidationNotification<UserDomainError>> result =
          userService.createUser(request.toCommand());

      assertThat(result.isFailure()).isTrue();
      assertThat(result.getError().getErrors())
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
      UserCreateRequest request = new UserCreateRequest("User", "mail@email.com", " ");
      when(userRepository.findByEmail(Email.of(request.email()).getValue()))
          .thenReturn(Optional.empty());

      Result<User, ValidationNotification<UserDomainError>> result =
          userService.createUser(request.toCommand());

      assertThat(result.isFailure()).isTrue();
      assertThat(result.getError().getErrors())
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
      UserCreateRequest request = new UserCreateRequest("User 1", "user1@mail.com", VALID_PASSWORD);
      User existing = buildUser(1, request.email());
      when(userRepository.findByEmail(Email.of(request.email()).getValue()))
          .thenReturn(Optional.of(existing));

      Result<User, ValidationNotification<UserDomainError>> result =
          userService.createUser(request.toCommand());

      assertThat(result.isFailure()).isTrue();
      assertHasFailureOn(result.getError(), "email", EmailAlreadyTakenError.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {"some@mail", "some-mail.com", "@mail.com"})
    @DisplayName(
        "createUser: given an invalid email format, then return InvalidFormat error on email")
    public void createUser_invalidEmail_returnInvalidFormatErrorOnEmail(String email) {
      UserCreateRequest request = new UserCreateRequest("User", email, VALID_PASSWORD);

      Result<User, ValidationNotification<UserDomainError>> result =
          userService.createUser(request.toCommand());

      assertThat(result.isFailure()).isTrue();
      assertThat(result.getError().getErrors())
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
      UserCreateRequest request = new UserCreateRequest("User", "mail@email.com", password);
      when(userRepository.findByEmail(Email.of(request.email()).getValue()))
          .thenReturn(Optional.empty());

      Result<User, ValidationNotification<UserDomainError>> result =
          userService.createUser(request.toCommand());

      assertThat(result.isFailure()).isTrue();
      assertThat(result.getError().getErrors())
          .satisfiesExactly(
              e -> {
                assertThat(e.field()).isEqualTo("password");
                assertThat(e.error()).isInstanceOf(InvalidPasswordError.class);
              });
    }

    // ── updateUserById ────────────────────────────────────────────────────────

    @Test
    @DisplayName("updateUserById: given a non-existing id, then return NotFound error")
    public void updateUserById_nonExistingId_returnNotFoundError() {
      int id = 999;
      UpdateUserCommand command = new UpdateUserCommand("New Name", null, null);
      when(userRepository.findById(id)).thenReturn(Optional.empty());

      Result<User, UserService.UpdateUserByIdError> result =
          userService.updateUserById(id, command);

      assertThat(result.isFailure()).isTrue();
      assertThat(result.getError())
          .isInstanceOf(UserService.UpdateUserByIdError.NotFound.class)
          .extracting(e -> ((UserService.UpdateUserByIdError.NotFound) e).error().getReason())
          .isEqualTo(new UserNotFoundError.Reason.ById(id));
    }

    @Test
    @DisplayName("updateUserById: given all fields empty, then return EmptyCommand error")
    public void updateUserById_emptyCommand_returnEmptyCommandFailure() {
      UpdateUserCommand command = new UpdateUserCommand(null, null, null);

      Result<User, UserService.UpdateUserByIdError> result = userService.updateUserById(1, command);

      assertThat(result.isFailure()).isTrue();
      assertThat(result.getError())
          .isInstanceOf(UserService.UpdateUserByIdError.EmptyCommand.class);
    }

    @Test
    @DisplayName("updateUserById: given a blank name, then return Blank error on name")
    public void updateUserById_blankName_returnBlankErrorOnName() {
      User existing = buildUser(1, "previous@mail.com");
      UpdateUserCommand command = new UpdateUserCommand(" ", null, VALID_PASSWORD);
      when(userRepository.findById(existing.id())).thenReturn(Optional.of(existing));

      Result<User, UserService.UpdateUserByIdError> result =
          userService.updateUserById(existing.id(), command);

      assertThat(result.isFailure()).isTrue();
      assertThat(result.getError())
          .isInstanceOf(UserService.UpdateUserByIdError.ValidationFailed.class);
      ValidationNotification<UserDomainError> n =
          ((UserService.UpdateUserByIdError.ValidationFailed) result.getError()).notification();

      assertThat(n.getErrors())
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

      Result<User, UserService.UpdateUserByIdError> result =
          userService.updateUserById(existing.id(), command);

      assertThat(result.isFailure()).isTrue();
      assertThat(result.getError())
          .isInstanceOf(UserService.UpdateUserByIdError.ValidationFailed.class);
      ValidationNotification<UserDomainError> n =
          ((UserService.UpdateUserByIdError.ValidationFailed) result.getError()).notification();

      assertThat(n.getErrors())
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

      Result<User, UserService.UpdateUserByIdError> result =
          userService.updateUserById(existing.id(), command);

      assertThat(result.isFailure()).isTrue();
      assertThat(result.getError())
          .isInstanceOf(UserService.UpdateUserByIdError.ValidationFailed.class);
      ValidationNotification<UserDomainError> n =
          ((UserService.UpdateUserByIdError.ValidationFailed) result.getError()).notification();

      assertThat(n.getErrors())
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

      Result<User, UserService.UpdateUserByIdError> result =
          userService.updateUserById(existing.id(), command);

      assertThat(result.isFailure()).isTrue();
      assertThat(result.getError())
          .isInstanceOf(UserService.UpdateUserByIdError.ValidationFailed.class);
      ValidationNotification<UserDomainError> n =
          ((UserService.UpdateUserByIdError.ValidationFailed) result.getError()).notification();

      assertThat(n.getErrors())
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

      Result<User, UserService.UpdateUserByIdError> result =
          userService.updateUserById(existing.id(), command);

      assertThat(result.isFailure()).isTrue();
      assertThat(result.getError())
          .isInstanceOf(UserService.UpdateUserByIdError.ValidationFailed.class);
      ValidationNotification<UserDomainError> n =
          ((UserService.UpdateUserByIdError.ValidationFailed) result.getError()).notification();

      assertThat(n.getErrors())
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
      when(userRepository.findByEmail(Email.of(command.email()).getValue()))
          .thenReturn(Optional.of(existing));

      Result<User, UserService.UpdateUserByIdError> result = userService.updateUserById(1, command);

      assertThat(result.isFailure()).isTrue();
      assertThat(result.getError())
          .isInstanceOf(UserService.UpdateUserByIdError.ValidationFailed.class);
      ValidationNotification<UserDomainError> n =
          ((UserService.UpdateUserByIdError.ValidationFailed) result.getError()).notification();

      assertHasFailureOn(n, "email", EmailAlreadyTakenError.class);
    }

    @Test
    @DisplayName("deleteUserById: given a non-existing id, then return UserNotFoundError")
    public void deleteUserById_nonExistingId_returnUserNotFoundError() {
      int id = 999;
      when(userRepository.deleteById(id)).thenReturn(false);

      Result<Void, UserNotFoundError> result = userService.deleteUserById(id);

      assertThat(result.isFailure()).isTrue();
      assertThat(result.getError())
          .isInstanceOf(UserNotFoundError.class)
          .extracting(ReasonedError::getReason)
          .isEqualTo(new UserNotFoundError.Reason.ById(id));
    }
  }
}
