package com.anibalxyz.features.users.application;

import static com.anibalxyz.features.Constants.Environment.BCRYPT_LOG_ROUNDS;
import static com.anibalxyz.features.Constants.Users.VALID_PASSWORD;
import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.when;

import com.anibalxyz.features.Constants;
import com.anibalxyz.features.common.Result;
import com.anibalxyz.features.common.application.ValidationNotification;
import com.anibalxyz.features.common.domain.error.DomainError;
import com.anibalxyz.features.users.api.in.UserCreateRequest;
import com.anibalxyz.features.users.api.in.UserUpdateRequest;
import com.anibalxyz.features.users.domain.Email;
import com.anibalxyz.features.users.domain.PasswordHash;
import com.anibalxyz.features.users.domain.User;
import com.anibalxyz.features.users.domain.UserRepository;
import com.anibalxyz.features.users.domain.error.InvalidEmailError;
import com.anibalxyz.features.users.domain.error.InvalidPasswordError;
import com.anibalxyz.features.users.domain.error.UserNotFoundError;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("Tests for UserService")
public class UserServiceTest {
  @Mock private UserRepository userRepository;

  private UserService userService;

  @BeforeAll
  public static void setup() {
    Constants.init();
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
      Instant currentDate = Instant.now();
      List<User> expectedUsers =
          List.of(
              new User(
                  1,
                  "User 1",
                  Email.of("user1@mail.com").getValue(),
                  PasswordHash.generate(VALID_PASSWORD, BCRYPT_LOG_ROUNDS).getValue(),
                  currentDate,
                  currentDate),
              new User(
                  2,
                  "User 2",
                  Email.of("user2@mail.com").getValue(),
                  PasswordHash.generate(VALID_PASSWORD, BCRYPT_LOG_ROUNDS).getValue(),
                  currentDate,
                  currentDate));
      when(userRepository.findAll()).thenReturn(expectedUsers);

      List<User> actualUsers = userService.getAllUsers();

      assertThat(actualUsers).isEqualTo(expectedUsers);
    }

    @Test
    @DisplayName("getAllUsers: given no users exist, then return an empty list")
    public void getAllUsers_noUsersExist_returnEmptyList() {
      List<User> expectedResult = List.of();
      when(userRepository.findAll()).thenReturn(expectedResult);

      List<User> actualResult = userService.getAllUsers();

      assertThat(actualResult).isEqualTo(expectedResult);
    }

    @Test
    @DisplayName("getUserById: given an existing id, then return the correct user")
    public void getUserById_existingId_returnUser() {
      Instant currentDate = Instant.now();
      User expectedUser =
          new User(
              1,
              "User 1",
              Email.of("user1@mail.com").getValue(),
              PasswordHash.generate(VALID_PASSWORD, BCRYPT_LOG_ROUNDS).getValue(),
              currentDate,
              currentDate);
      when(userRepository.findById(expectedUser.getId())).thenReturn(Optional.of(expectedUser));

      User actualUser = userService.getUserById(expectedUser.getId()).getValue();

      assertThat(actualUser).isEqualTo(expectedUser);
    }

    @Test
    @DisplayName("getUserByEmail: given an existing email, then return the correct user")
    public void getUserByEmail_existingEmail_returnUser() {
      Instant currentDate = Instant.now();
      User expectedUser =
          new User(
              1,
              "User 1",
              Email.of("user1@mail.com").getValue(),
              PasswordHash.generate(VALID_PASSWORD, BCRYPT_LOG_ROUNDS).getValue(),
              currentDate,
              currentDate);
      when(userRepository.findByEmail(Email.of(expectedUser.getEmail().value()).getValue()))
          .thenReturn(Optional.of(expectedUser));

      User actualUser = userService.getUserByEmail(expectedUser.getEmail().value()).getValue();

      assertThat(actualUser).isEqualTo(expectedUser);
    }

    @Test
    @DisplayName("createUser: given valid data, then return the created user")
    public void createUser_validData_returnCreatedUser() {
      int validId = 1;
      Instant currentDate = Instant.now();
      UserCreateRequest request = new UserCreateRequest("User 1", "user1@mail.com", VALID_PASSWORD);
      User creatingUser =
          new User(
              request.name(),
              Email.of(request.email()).getValue(),
              PasswordHash.generate(request.password(), BCRYPT_LOG_ROUNDS).getValue());
      User expectedUser =
          creatingUser.withId(validId).withCreatedAt(currentDate).withUpdatedAt(currentDate);

      when(userRepository.findByEmail(expectedUser.getEmail())).thenReturn(Optional.empty());
      when(userRepository.save(
              argThat(
                  user ->
                      user.getName().equals(creatingUser.getName())
                          && user.getEmail().equals(creatingUser.getEmail())
                          && user.getPasswordHash().matches(request.password()))))
          .thenAnswer(invocation -> invocation.getArgument(0));

      User actualUser =
          userService
              .createUser(request.toCommand())
              .getValue()
              .withId(validId)
              .withCreatedAt(currentDate)
              .withUpdatedAt(currentDate);

      assertThat(actualUser.getName()).isEqualTo(expectedUser.getName());
      assertThat(actualUser.getEmail()).isEqualTo(expectedUser.getEmail());
      assertTrue(actualUser.getPasswordHash().matches(request.password()));
      assertThat(actualUser.getUpdatedAt()).isEqualTo(expectedUser.getUpdatedAt());
      assertThat(actualUser.getCreatedAt()).isEqualTo(expectedUser.getCreatedAt());
    }

    @Test
    @DisplayName("updateUserById: given a valid id and name, then return the updated user")
    public void updateUserById_validIdAndName_returnUpdatedUser() {
      int existingId = 1;
      UserUpdateRequest request = new UserUpdateRequest("New Name", null, null);
      Instant currentDate = Instant.now();
      User existingUser =
          new User(
              existingId,
              "Previous",
              Email.of("previous@mail.com").getValue(),
              PasswordHash.generate(VALID_PASSWORD, BCRYPT_LOG_ROUNDS).getValue(),
              currentDate,
              currentDate);

      User expectedUpdatedUser = existingUser.withName(request.name());

      when(userRepository.findById(existingId)).thenReturn(Optional.of(existingUser));
      when(userRepository.save(expectedUpdatedUser))
          .thenAnswer(invocation -> invocation.getArgument(0));

      User actualUpdatedUser =
          userService.updateUserById(existingId, request.toCommand()).getValue();

      assertThat(actualUpdatedUser).isEqualTo(expectedUpdatedUser);
    }

    @Test
    @DisplayName("updateUserById: given a valid id and email, then return the updated user")
    public void updateUserById_validIdAndEmail_returnUpdatedUser() {
      int existingId = 1;
      UserUpdateRequest request = new UserUpdateRequest(null, "new@mail.com", null);
      Instant currentDate = Instant.now();
      User existingUser =
          new User(
              existingId,
              "Previous",
              Email.of("previous@mail.com").getValue(),
              PasswordHash.generate(VALID_PASSWORD, BCRYPT_LOG_ROUNDS).getValue(),
              currentDate,
              currentDate);

      User expectedUpdatedUser = existingUser.withEmail(Email.of(request.email()).getValue());

      when(userRepository.findById(existingId)).thenReturn(Optional.of(existingUser));
      when(userRepository.findByEmail(expectedUpdatedUser.getEmail())).thenReturn(Optional.empty());
      when(userRepository.save(expectedUpdatedUser))
          .thenAnswer(invocation -> invocation.getArgument(0));

      User actualUpdatedUser =
          userService.updateUserById(existingId, request.toCommand()).getValue();

      assertThat(actualUpdatedUser).isEqualTo(expectedUpdatedUser);
    }

    @Test
    @DisplayName("updateUserById: given a valid id and password, then return the updated user")
    public void updateUserById_validIdAndPassword_returnUpdatedUser() {
      int existingId = 1;
      UserUpdateRequest payload = new UserUpdateRequest(null, null, "new" + VALID_PASSWORD);
      Instant currentDate = Instant.now();
      User existingUser =
          new User(
              existingId,
              "Previous",
              Email.of("previous@email.com").getValue(),
              PasswordHash.generate(VALID_PASSWORD, BCRYPT_LOG_ROUNDS).getValue(),
              currentDate,
              currentDate);

      User expectedUpdatedUser =
          existingUser.withPasswordHash(
              PasswordHash.generate(payload.password(), BCRYPT_LOG_ROUNDS).getValue());

      when(userRepository.findById(existingId)).thenReturn(Optional.of(existingUser));
      when(userRepository.save(
              argThat(
                  user ->
                      user.getId() == existingId
                          && user.getEmail().equals(expectedUpdatedUser.getEmail())
                          && user.getPasswordHash().matches(payload.password())
                          && user.getUpdatedAt().equals(expectedUpdatedUser.getUpdatedAt())
                          && user.getCreatedAt().equals(expectedUpdatedUser.getCreatedAt()))))
          .thenAnswer(invocation -> invocation.getArgument(0));

      User actualUpdatedUser =
          userService.updateUserById(existingId, payload.toCommand()).getValue();

      assertThat(actualUpdatedUser.getName()).isEqualTo(expectedUpdatedUser.getName());
      assertThat(actualUpdatedUser.getEmail()).isEqualTo(expectedUpdatedUser.getEmail());
      assertTrue(actualUpdatedUser.getPasswordHash().matches(payload.password()));
      assertThat(actualUpdatedUser.getUpdatedAt()).isEqualTo(expectedUpdatedUser.getUpdatedAt());
      assertThat(actualUpdatedUser.getCreatedAt()).isEqualTo(expectedUpdatedUser.getCreatedAt());
    }

    @Test
    @DisplayName(
        "updateUserById: given an email already in use by the user, then return the unmodified user")
    public void updateUserById_emailAlreadyUsedByUser_returnUnmodifiedUser() {
      int updatingId = 1;
      UserUpdateRequest request = new UserUpdateRequest(null, "updating@mail.com", null);
      Instant now = Instant.now();
      User existingUser =
          new User(
              updatingId,
              "Previous",
              Email.of(request.email()).getValue(),
              PasswordHash.generate(VALID_PASSWORD, BCRYPT_LOG_ROUNDS).getValue(),
              now,
              now);

      when(userRepository.findById(updatingId)).thenReturn(Optional.of(existingUser));
      when(userRepository.save(existingUser)).thenAnswer(invocation -> invocation.getArgument(0));

      User updatedUser = userService.updateUserById(updatingId, request.toCommand()).getValue();

      assertThat(updatedUser).isEqualTo(existingUser);
    }

    @Test
    @DisplayName("deleteUserById: given an existing id, then delete user")
    public void deleteUserById_existingId_deleteUser() {
      int existingId = 1;
      when(userRepository.deleteById(existingId)).thenReturn(true);

      assertThatCode(() -> userService.deleteUserById(existingId)).doesNotThrowAnyException();
    }
  }

  @Nested
  @DisplayName("Failure Scenarios")
  class FailureScenarios {

    @Test
    @DisplayName("getUserById: given a non-existing id, then throw ResourceNotFoundException")
    public void getUserById_nonExistingId_throwResourceNotFoundException() {
      int nonExistingId = 999;
      when(userRepository.findById(nonExistingId)).thenReturn(Optional.empty());

      Result<User, UserNotFoundError> result = userService.getUserById(nonExistingId);

      assertThat(result.isFailure()).isTrue();
      assertThat(result.getError()).isInstanceOf(UserNotFoundError.class);
    }

    @Test
    @DisplayName("getUserByEmail: given a non-existing email, then throw ResourceNotFoundException")
    public void getUserByEmail_nonExistingEmail_throwResourceNotFoundException() {
      String nonExistingEmail = "non.existing@mail.com";
      when(userRepository.findByEmail(Email.of(nonExistingEmail).getValue()))
          .thenReturn(Optional.empty());

      Result<User, DomainError> result = userService.getUserByEmail(nonExistingEmail);

      assertThat(result.isFailure()).isTrue();
      assertThat(result.getError()).isInstanceOf(UserNotFoundError.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {"blank", "format"})
    @DisplayName("getUserByEmail: given an invalid email format, then throw InvalidInputException")
    public void getUserByEmail_invalidEmailFormat_throwInvalidInputException(
        String invalidationCause) {
      String email = invalidationCause.equals("format") ? "mailemail.com" : " ";

      Result<User, DomainError> result = userService.getUserByEmail(email);

      assertThat(result.isFailure()).isTrue();
      assertThat(result.getError()).isInstanceOf(InvalidEmailError.class);
    }

    @Test
    @DisplayName("createUser: given an existing email, then throw ConflictException")
    public void createUser_existingEmail_throwConflictException() {
      Instant currentDate = Instant.now();
      UserCreateRequest request = new UserCreateRequest("User 1", "user1@mail.com", VALID_PASSWORD);
      User existingUser =
          new User(
              1,
              request.name(),
              Email.of(request.email()).getValue(),
              PasswordHash.generate(request.password(), BCRYPT_LOG_ROUNDS).getValue(),
              currentDate,
              currentDate);
      when(userRepository.findByEmail(Email.of(request.email()).getValue()))
          .thenReturn(Optional.of(existingUser));

      Result<User, ValidationNotification> result = userService.createUser(request.toCommand());

      assertThat(result.isFailure()).isTrue();
      assertThat(result.getError().getErrors())
          .satisfiesExactly(
              errorEntry -> {
                assertThat(errorEntry.field()).isEqualTo("email");
                assertThat(errorEntry.failure())
                    .isInstanceOf(ValidationNotification.FieldFailure.Conflict.class);
              });
    }

    @ParameterizedTest
    @ValueSource(strings = {"some@mail", "some-mail.com", "@mail.com"})
    @DisplayName("createUser: given an invalid email format, then throw InvalidInputException")
    public void createUser_invalidEmailFormat_throwInvalidInputException(String email) {
      UserCreateRequest request = new UserCreateRequest("User", email, VALID_PASSWORD);

      Result<User, ValidationNotification> result = userService.createUser(request.toCommand());

      assertThat(result.isFailure()).isTrue();
      assertThat(result.getError().getErrors())
          .satisfiesExactly(
              errorEntry -> {
                assertThat(errorEntry.field()).isEqualTo("email");
                assertThat(errorEntry.failure())
                    .isInstanceOf(ValidationNotification.FieldFailure.InvalidValue.class);
              });
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" "})
    @DisplayName("createUser: given a missing password, returns Missing failure")
    public void createUser_missingPassword_returnsMissingFailure(String password) {
      UserCreateRequest request = new UserCreateRequest("User", "mail@email.com", password);
      when(userRepository.findByEmail(Email.of(request.email()).getValue()))
          .thenReturn(Optional.empty());

      Result<User, ValidationNotification> result = userService.createUser(request.toCommand());

      assertThat(result.isFailure()).isTrue();
      assertThat(result.getError().getErrors())
          .satisfiesExactly(
              errorEntry -> {
                assertThat(errorEntry.field()).isEqualTo("password");
                assertThat(errorEntry.failure())
                    .isInstanceOf(ValidationNotification.FieldFailure.Missing.class);
              });
    }

    @ParameterizedTest
    @ValueSource(
        strings = {
          "short",
          "long-72-chars-12345678901234567890123456789012345678901234567890123456789"
        })
    @DisplayName(
        "createUser: given an invalid password value, returns InvalidValue failure with InvalidPasswordError")
    public void createUser_invalidPasswordValue_returnsInvalidValueFailure(String password) {
      UserCreateRequest request = new UserCreateRequest("User", "mail@email.com", password);
      when(userRepository.findByEmail(Email.of(request.email()).getValue()))
          .thenReturn(Optional.empty());

      Result<User, ValidationNotification> result = userService.createUser(request.toCommand());

      assertThat(result.isFailure()).isTrue();
      assertThat(result.getError().getErrors())
          .satisfiesExactly(
              errorEntry -> {
                assertThat(errorEntry.field()).isEqualTo("password");
                ValidationNotification.FieldFailure failure = errorEntry.failure();
                assertThat(failure)
                    .isInstanceOf(ValidationNotification.FieldFailure.InvalidValue.class);
                assertThat(((ValidationNotification.FieldFailure.InvalidValue) failure).error())
                    .isInstanceOf(InvalidPasswordError.class);
              });
    }

    @Test
    @DisplayName("updateUserById: given a non-existing id, then throw ResourceNotFoundException")
    public void updateUserById_nonExistingId_throwResourceNotFoundException() {
      int nonExistingId = 999;
      UserUpdateRequest request = new UserUpdateRequest("New Name", null, null);

      when(userRepository.findById(nonExistingId)).thenReturn(Optional.empty());

      Result<User, UserService.UpdateUserByIdError> result =
          userService.updateUserById(nonExistingId, request.toCommand());

      assertThat(result.isFailure()).isTrue();
      assertThat(result.getError()).isInstanceOf(UserService.UpdateUserByIdError.NotFound.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {"some@mail", "some-mail.com", "@mail.com"})
    @DisplayName("updateUserById: given an invalid email format, then throw InvalidInputException")
    public void updateUserById_invalidEmailFormat_throwInvalidInputException(String email) {
      int existingId = 1;
      UserUpdateRequest request = new UserUpdateRequest(null, email, null);
      Instant currentDate = Instant.now();
      User existingUser =
          new User(
              existingId,
              "Previous",
              Email.of("previous@mail.com").getValue(),
              PasswordHash.generate(VALID_PASSWORD, BCRYPT_LOG_ROUNDS).getValue(),
              currentDate,
              currentDate);

      when(userRepository.findById(existingId)).thenReturn(Optional.of(existingUser));

      Result<User, UserService.UpdateUserByIdError> result =
          userService.updateUserById(existingId, request.toCommand());

      assertThat(result.isFailure()).isTrue();
      assertThat(result.getError())
          .isInstanceOf(UserService.UpdateUserByIdError.ValidationFailed.class);

      ValidationNotification notification =
          ((UserService.UpdateUserByIdError.ValidationFailed) result.getError()).notification();

      assertThat(notification.getErrors())
          .satisfiesExactly(
              errorEntry -> {
                assertThat(errorEntry.field()).isEqualTo("email");
                assertThat(errorEntry.failure())
                    .isInstanceOf(ValidationNotification.FieldFailure.InvalidValue.class);
              });
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" "})
    @DisplayName("updateUserById: given all fields empty, returns EmptyCommand failure")
    public void updateUserById_emptyCommand_returnsEmptyCommandFailure(String password) {
      int existingId = 1;
      UserUpdateRequest request = new UserUpdateRequest(null, null, password);

      Result<User, UserService.UpdateUserByIdError> result =
          userService.updateUserById(existingId, request.toCommand());

      assertThat(result.isFailure()).isTrue();
      assertThat(result.getError())
          .isInstanceOf(UserService.UpdateUserByIdError.EmptyCommand.class);
    }

    @ParameterizedTest
    @ValueSource(
        strings = {
          "short",
          "long-72-chars-12345678901234567890123456789012345678901234567890123456789"
        })
    @DisplayName(
        "updateUserById: given an invalid password value, returns ValidationFailed with InvalidPasswordError")
    public void updateUserById_invalidPasswordValue_returnsValidationFailedWithInvalidPasswordError(
        String password) {
      int existingId = 1;
      UserUpdateRequest request = new UserUpdateRequest(null, null, password);
      Instant currentDate = Instant.now();
      User existingUser =
          new User(
              existingId,
              "Previous",
              Email.of("previous@mail.com").getValue(),
              PasswordHash.generate(VALID_PASSWORD, BCRYPT_LOG_ROUNDS).getValue(),
              currentDate,
              currentDate);

      when(userRepository.findById(existingId)).thenReturn(Optional.of(existingUser));

      Result<User, UserService.UpdateUserByIdError> result =
          userService.updateUserById(existingId, request.toCommand());

      assertThat(result.isFailure()).isTrue();
      assertThat(result.getError())
          .isInstanceOf(UserService.UpdateUserByIdError.ValidationFailed.class);

      ValidationNotification notification =
          ((UserService.UpdateUserByIdError.ValidationFailed) result.getError()).notification();

      assertThat(notification.getErrors())
          .satisfiesExactly(
              errorEntry -> {
                assertThat(errorEntry.field()).isEqualTo("password");
                ValidationNotification.FieldFailure failure = errorEntry.failure();
                assertThat(failure)
                    .isInstanceOf(ValidationNotification.FieldFailure.InvalidValue.class);
                assertThat(((ValidationNotification.FieldFailure.InvalidValue) failure).error())
                    .isInstanceOf(InvalidPasswordError.class);
              });
    }

    @Test
    @DisplayName(
        "updateUserById: given an email already in use by another user, then throw ConflictException")
    public void updateUserById_emailAlreadyUsedByAnotherUser_throwConflictException() {
      int updatingId = 1;
      UserUpdateRequest request = new UserUpdateRequest(null, "updating@mail.com", null);
      Instant now = Instant.now();
      User existingUser =
          new User(
              2,
              "Previous",
              Email.of("previous@mail.com").getValue(),
              PasswordHash.generate(VALID_PASSWORD, BCRYPT_LOG_ROUNDS).getValue(),
              now,
              now);

      when(userRepository.findById(updatingId)).thenReturn(Optional.of(existingUser));
      when(userRepository.findByEmail(Email.of(request.email()).getValue()))
          .thenReturn(Optional.of(existingUser));

      Result<User, UserService.UpdateUserByIdError> result =
          userService.updateUserById(updatingId, request.toCommand());

      assertThat(result.isFailure()).isTrue();
      assertThat(result.getError())
          .isInstanceOf(UserService.UpdateUserByIdError.ValidationFailed.class);

      ValidationNotification notification =
          ((UserService.UpdateUserByIdError.ValidationFailed) result.getError()).notification();

      assertThat(notification.getErrors())
          .satisfiesExactly(
              errorEntry ->
                  assertThat(errorEntry.failure())
                      .isInstanceOf(ValidationNotification.FieldFailure.Conflict.class));
    }

    @Test
    @DisplayName("deleteUserById: given a non-existing id, then throw ResourceNotFoundException")
    public void deleteUserById_nonExistingId_throwResourceNotFoundException() {
      int nonExistingId = 999;
      when(userRepository.deleteById(nonExistingId)).thenReturn(false);

      Result<Void, UserNotFoundError> result = userService.deleteUserById(nonExistingId);

      assertThat(result.isFailure()).isTrue();
      assertThat(result.getError()).isInstanceOf(UserNotFoundError.class);
    }
  }
}
