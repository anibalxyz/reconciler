package com.anibalxyz.features.users.application;

import static com.anibalxyz.features.Constants.Environment.BCRYPT_LOG_ROUNDS;
import static com.anibalxyz.features.Constants.Users.VALID_PASSWORD;
import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.when;

import com.anibalxyz.features.Constants;
import com.anibalxyz.features.common.application.exception.ConflictException;
import com.anibalxyz.features.common.application.exception.InvalidInputException;
import com.anibalxyz.features.common.application.exception.ResourceNotFoundException;
import com.anibalxyz.features.users.application.in.UserUpdatePayload;
import com.anibalxyz.features.users.domain.Email;
import com.anibalxyz.features.users.domain.PasswordHash;
import com.anibalxyz.features.users.domain.User;
import com.anibalxyz.features.users.domain.UserRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EmptySource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("Tests for UserService")
public class UserServiceTest {
  @Mock private UserRepository userRepository;

  private UserService userService;

  private static UserUpdatePayload createPayload(String name, String email, String password) {
    return new UserUpdatePayload() {
      @Override
      public String name() {
        return name;
      }

      @Override
      public String email() {
        return email;
      }

      @Override
      public String password() {
        return password;
      }
    };
  }

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
                  new Email("user1@mail.com"),
                  PasswordHash.generate(VALID_PASSWORD, BCRYPT_LOG_ROUNDS),
                  currentDate,
                  currentDate),
              new User(
                  2,
                  "User 2",
                  new Email("user2@mail.com"),
                  PasswordHash.generate(VALID_PASSWORD, BCRYPT_LOG_ROUNDS),
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
              new Email("user1@mail.com"),
              PasswordHash.generate(VALID_PASSWORD, BCRYPT_LOG_ROUNDS),
              currentDate,
              currentDate);
      when(userRepository.findById(expectedUser.getId())).thenReturn(Optional.of(expectedUser));

      User actualUser = userService.getUserById(expectedUser.getId());

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
              new Email("user1@mail.com"),
              PasswordHash.generate(VALID_PASSWORD, BCRYPT_LOG_ROUNDS),
              currentDate,
              currentDate);
      when(userRepository.findByEmail(new Email(expectedUser.getEmail().value())))
          .thenReturn(Optional.of(expectedUser));

      User actualUser = userService.getUserByEmail(expectedUser.getEmail().value());

      assertThat(actualUser).isEqualTo(expectedUser);
    }

    @Test
    @DisplayName("createUser: given valid data, then return the created user")
    public void createUser_validData_returnCreatedUser() {
      int validId = 1;
      Instant currentDate = Instant.now();
      UserUpdatePayload payload = createPayload("User 1", "user1@mail.com", VALID_PASSWORD);
      User creatingUser =
          new User(
              payload.name(),
              new Email(payload.email()),
              PasswordHash.generate(payload.password(), BCRYPT_LOG_ROUNDS));
      User expectedUser =
          creatingUser.withId(validId).withCreatedAt(currentDate).withUpdatedAt(currentDate);

      when(userRepository.findByEmail(expectedUser.getEmail())).thenReturn(Optional.empty());
      when(userRepository.save(
              argThat(
                  user ->
                      user.getName().equals(creatingUser.getName())
                          && user.getEmail().equals(creatingUser.getEmail())
                          && user.getPasswordHash().matches(payload.password()))))
          .thenAnswer(invocation -> invocation.getArgument(0));

      User actualUser =
          userService
              .createUser(payload)
              .withId(validId)
              .withCreatedAt(currentDate)
              .withUpdatedAt(currentDate);

      assertThat(actualUser.getName()).isEqualTo(expectedUser.getName());
      assertThat(actualUser.getEmail()).isEqualTo(expectedUser.getEmail());
      assertTrue(actualUser.getPasswordHash().matches(payload.password()));
      assertThat(actualUser.getUpdatedAt()).isEqualTo(expectedUser.getUpdatedAt());
      assertThat(actualUser.getCreatedAt()).isEqualTo(expectedUser.getCreatedAt());
    }

    @Test
    @DisplayName("updateUserById: given a valid id and name, then return the updated user")
    public void updateUserById_validIdAndName_returnUpdatedUser() {
      int existingId = 1;
      UserUpdatePayload payload = createPayload("New Name", null, null);
      Instant currentDate = Instant.now();
      User existingUser =
          new User(
              existingId,
              "Previous",
              new Email("previous@mail.com"),
              PasswordHash.generate(VALID_PASSWORD, BCRYPT_LOG_ROUNDS),
              currentDate,
              currentDate);

      User expectedUpdatedUser = existingUser.withName(payload.name());

      when(userRepository.findById(existingId)).thenReturn(Optional.of(existingUser));
      when(userRepository.save(expectedUpdatedUser))
          .thenAnswer(invocation -> invocation.getArgument(0));

      User actualUpdatedUser = userService.updateUserById(existingId, payload);

      assertThat(actualUpdatedUser).isEqualTo(expectedUpdatedUser);
    }

    @Test
    @DisplayName("updateUserById: given a valid id and email, then return the updated user")
    public void updateUserById_validIdAndEmail_returnUpdatedUser() {
      int existingId = 1;
      UserUpdatePayload payload = createPayload(null, "new@mail.com", null);
      Instant currentDate = Instant.now();
      User existingUser =
          new User(
              existingId,
              "Previous",
              new Email("previous@mail.com"),
              PasswordHash.generate(VALID_PASSWORD, BCRYPT_LOG_ROUNDS),
              currentDate,
              currentDate);

      User expectedUpdatedUser = existingUser.withEmail(new Email(payload.email()));

      when(userRepository.findById(existingId)).thenReturn(Optional.of(existingUser));
      when(userRepository.findByEmail(expectedUpdatedUser.getEmail())).thenReturn(Optional.empty());
      when(userRepository.save(expectedUpdatedUser))
          .thenAnswer(invocation -> invocation.getArgument(0));

      User actualUpdatedUser = userService.updateUserById(existingId, payload);

      assertThat(actualUpdatedUser).isEqualTo(expectedUpdatedUser);
    }

    @Test
    @DisplayName("updateUserById: given a valid id and password, then return the updated user")
    public void updateUserById_validIdAndPassword_returnUpdatedUser() {
      int existingId = 1;
      UserUpdatePayload payload = createPayload(null, null, "new" + VALID_PASSWORD);
      Instant currentDate = Instant.now();
      User existingUser =
          new User(
              existingId,
              "Previous",
              new Email("previous@email.com"),
              PasswordHash.generate(VALID_PASSWORD, BCRYPT_LOG_ROUNDS),
              currentDate,
              currentDate);

      User expectedUpdatedUser =
          existingUser.withPasswordHash(
              PasswordHash.generate(payload.password(), BCRYPT_LOG_ROUNDS));

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

      User actualUpdatedUser = userService.updateUserById(existingId, payload);

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
      UserUpdatePayload payload = createPayload(null, "updating@mail.com", null);
      Instant now = Instant.now();
      User existingUser =
          new User(
              updatingId,
              "Previous",
              new Email(payload.email()),
              PasswordHash.generate(VALID_PASSWORD, BCRYPT_LOG_ROUNDS),
              now,
              now);

      when(userRepository.findById(updatingId)).thenReturn(Optional.of(existingUser));
      when(userRepository.save(existingUser)).thenAnswer(invocation -> invocation.getArgument(0));

      User updatedUser = userService.updateUserById(updatingId, payload);

      assertThat(updatedUser).isEqualTo(existingUser);
    }

    @Test
    @DisplayName("updateUserById: given an empty payload, then return the unmodified user")
    public void updateUserById_emptyPayload_returnSameUser() {
      int existingId = 1;
      UserUpdatePayload payload = createPayload(null, null, null);
      Instant currentDate = Instant.now();
      User existingUser =
          new User(
              existingId,
              "Previous",
              new Email("previous@email.com"),
              PasswordHash.generate(VALID_PASSWORD, BCRYPT_LOG_ROUNDS),
              currentDate,
              currentDate);

      when(userRepository.findById(existingId)).thenReturn(Optional.of(existingUser));
      when(userRepository.save(existingUser)).thenAnswer(invocation -> invocation.getArgument(0));

      User actualUser = userService.updateUserById(existingId, payload);

      assertThat(actualUser).isEqualTo(existingUser);
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

      assertThatThrownBy(() -> userService.getUserById(nonExistingId))
          .isInstanceOf(ResourceNotFoundException.class)
          .hasMessage("User with id " + nonExistingId + " not found");
    }

    @Test
    @DisplayName("getUserByEmail: given a non-existing email, then throw ResourceNotFoundException")
    public void getUserByEmail_nonExistingEmail_throwResourceNotFoundException() {
      String nonExistingEmail = "non.existing@mail.com";
      when(userRepository.findByEmail(new Email(nonExistingEmail))).thenReturn(Optional.empty());

      assertThatThrownBy(() -> userService.getUserByEmail(nonExistingEmail))
          .isInstanceOf(ResourceNotFoundException.class)
          .hasMessage("User with email " + nonExistingEmail + " not found");
    }

    @ParameterizedTest
    @CsvSource({"blank", "format"})
    @DisplayName("getUserByEmail: given an invalid email format, then throw InvalidInputException")
    public void getUserByEmail_invalidEmailFormat_throwInvalidInputException(
        String invalidationCause) {
      String email = invalidationCause.equals("format") ? "mailemail.com" : " ";

      assertThatThrownBy(() -> userService.getUserByEmail(email))
          .isInstanceOf(InvalidInputException.class)
          .hasMessage("Invalid email format: " + email);
    }

    @Test
    @DisplayName("createUser: given an existing email, then throw ConflictException")
    public void createUser_existingEmail_throwConflictException() {
      Instant currentDate = Instant.now();
      UserUpdatePayload payload = createPayload("User 1", "user1@mail.com", VALID_PASSWORD);
      User existingUser =
          new User(
              1,
              payload.name(),
              new Email(payload.email()),
              PasswordHash.generate(payload.password(), BCRYPT_LOG_ROUNDS),
              currentDate,
              currentDate);
      when(userRepository.findByEmail(new Email(payload.email())))
          .thenReturn(Optional.of(existingUser));

      assertThatThrownBy(() -> userService.createUser(payload))
          .isInstanceOf(ConflictException.class)
          .hasMessage("Email already in use. Please use another");
    }

    @ParameterizedTest
    @CsvSource({"blank", "format"})
    @DisplayName("createUser: given an invalid email format, then throw InvalidInputException")
    public void createUser_invalidEmailFormat_throwInvalidInputException(String invalidationCause) {
      String email = invalidationCause.equals("format") ? "mailemail.com" : " ";
      UserUpdatePayload payload = createPayload("User", email, VALID_PASSWORD);

      assertThatThrownBy(() -> userService.createUser(payload))
          .isInstanceOf(InvalidInputException.class)
          .hasMessage("Invalid email format: " + payload.email());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "invalid"})
    @DisplayName("createUser: given an invalid password, then throw InvalidInputException")
    public void createUser_invalidPassword_throwInvalidPasswordFormatException(String password) {
      UserUpdatePayload payload = createPayload("User", "mail@email.com", password);
      when(userRepository.findByEmail(new Email(payload.email()))).thenReturn(Optional.empty());

      assertThatThrownBy(() -> userService.createUser(payload))
          .isInstanceOf(InvalidInputException.class);
    }

    @Test
    @DisplayName("updateUserById: given a non-existing id, then throw ResourceNotFoundException")
    public void updateUserById_nonExistingId_throwResourceNotFoundException() {
      int nonExistingId = 999;
      UserUpdatePayload payload = createPayload("New Name", null, null);

      when(userRepository.findById(nonExistingId)).thenReturn(Optional.empty());

      assertThatThrownBy(() -> userService.updateUserById(nonExistingId, payload))
          .isInstanceOf(ResourceNotFoundException.class)
          .hasMessage("User with id " + nonExistingId + " not found");
    }

    @ParameterizedTest
    @CsvSource({"blank", "format"})
    @DisplayName("updateUserById: given an invalid email format, then throw InvalidInputException")
    public void updateUserById_invalidEmailFormat_throwInvalidInputException(
        String invalidationCause) {
      int existingId = 1;
      String email = invalidationCause.equals("format") ? "invalidemail" : " ";
      UserUpdatePayload payload = createPayload(null, email, null);
      Instant currentDate = Instant.now();
      User existingUser =
          new User(
              existingId,
              "Previous",
              new Email("previous@mail.com"),
              PasswordHash.generate(VALID_PASSWORD, BCRYPT_LOG_ROUNDS),
              currentDate,
              currentDate);

      when(userRepository.findById(existingId)).thenReturn(Optional.of(existingUser));

      assertThatThrownBy(() -> userService.updateUserById(existingId, payload))
          .isInstanceOf(InvalidInputException.class)
          .hasMessage("Invalid email format: " + payload.email());
    }

    @ParameterizedTest
    @EmptySource
    @ValueSource(strings = {" ", "invalid"})
    @DisplayName(
        "updateUserById: given an invalid password format, then throw InvalidInputException")
    public void updateUserById_invalidPasswordFormat_throwInvalidPasswordFormatException(
        String password) {
      int existingId = 1;
      UserUpdatePayload payload = createPayload(null, null, password);
      Instant currentDate = Instant.now();
      User existingUser =
          new User(
              existingId,
              "Previous",
              new Email("previous@mail.com"),
              PasswordHash.generate(VALID_PASSWORD, BCRYPT_LOG_ROUNDS),
              currentDate,
              currentDate);

      when(userRepository.findById(existingId)).thenReturn(Optional.of(existingUser));

      // the message doesn't matter as it is being unit-tested
      assertThatThrownBy(() -> userService.updateUserById(existingId, payload))
          .isInstanceOf(InvalidInputException.class);
    }

    @Test
    @DisplayName(
        "updateUserById: given an email already in use by another user, then throw ConflictException")
    public void updateUserById_emailAlreadyUsedByAnotherUser_throwConflictException() {
      int updatingId = 1;
      UserUpdatePayload payload = createPayload(null, "updating@mail.com", null);
      Instant now = Instant.now();
      User existingUser =
          new User(
              2,
              "Previous",
              new Email("previous@mail.com"),
              PasswordHash.generate(VALID_PASSWORD, BCRYPT_LOG_ROUNDS),
              now,
              now);

      when(userRepository.findById(updatingId)).thenReturn(Optional.of(existingUser));
      when(userRepository.findByEmail(new Email(payload.email())))
          .thenReturn(Optional.of(existingUser));

      assertThatThrownBy(() -> userService.updateUserById(updatingId, payload))
          .isInstanceOf(ConflictException.class)
          .hasMessage("Email already in use. Please use another");
    }

    @Test
    @DisplayName("deleteUserById: given a non-existing id, then throw ResourceNotFoundException")
    public void deleteUserById_nonExistingId_throwResourceNotFoundException() {
      int nonExistingId = 999;
      when(userRepository.deleteById(nonExistingId)).thenReturn(false);

      assertThatThrownBy(() -> userService.deleteUserById(nonExistingId))
          .isInstanceOf(ResourceNotFoundException.class)
          .hasMessage("User with id " + nonExistingId + " not found");
    }
  }
}
