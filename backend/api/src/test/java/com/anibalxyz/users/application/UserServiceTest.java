package com.anibalxyz.users.application;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.when;

import com.anibalxyz.server.config.AppConfig;
import com.anibalxyz.server.config.EnvVarSet;
import com.anibalxyz.users.application.exception.EntityNotFoundException;
import com.anibalxyz.users.application.in.UserUpdatePayload;
import com.anibalxyz.users.domain.Email;
import com.anibalxyz.users.domain.PasswordHash;
import com.anibalxyz.users.domain.User;
import com.anibalxyz.users.domain.UserRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class UserServiceTest {
  private static final String VALID_PASSWORD = "V4L|D_Passw0Rd";

  private static EnvVarSet env;

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
    env = AppConfig.loadForTest().env();
  }

  @BeforeEach
  public void dependencyInjection() {
    userService = new UserService(userRepository, env);
  }

  @Nested
  class SuccessScenarios {

    @Test
    @DisplayName("getAllUsers: given users exist, then return a list of all users")
    public void getAllUsers_usersExist_returnListOfUsers() {
      LocalDateTime currentDate = LocalDateTime.now();
      List<User> expectedUsers =
          List.of(
              new User(
                  1,
                  "User 1",
                  new Email("user1@mail.com"),
                  PasswordHash.generate(VALID_PASSWORD, env.BCRYPT_LOG_ROUNDS()),
                  currentDate,
                  currentDate),
              new User(
                  2,
                  "User 2",
                  new Email("user2@mail.com"),
                  PasswordHash.generate(VALID_PASSWORD, env.BCRYPT_LOG_ROUNDS()),
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
      LocalDateTime currentDate = LocalDateTime.now();
      User expectedUser =
          new User(
              1,
              "User 1",
              new Email("user1@mail.com"),
              PasswordHash.generate(VALID_PASSWORD, env.BCRYPT_LOG_ROUNDS()),
              currentDate,
              currentDate);
      when(userRepository.findById(expectedUser.getId())).thenReturn(Optional.of(expectedUser));

      User actualUser = userService.getUserById(expectedUser.getId());

      assertThat(actualUser).isEqualTo(expectedUser);
    }

    @Test
    @DisplayName("createUser: given valid data, then return the created user")
    public void createUser_validData_returnCreatedUser() {
      int validId = 1;
      LocalDateTime currentDate = LocalDateTime.now();
      UserUpdatePayload payload = createPayload("User 1", "user1@mail.com", VALID_PASSWORD);
      User creatingUser =
          new User(
              payload.name(),
              new Email(payload.email()),
              PasswordHash.generate(payload.password(), env.BCRYPT_LOG_ROUNDS()));
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
      LocalDateTime currentDate = LocalDateTime.now();
      User existingUser =
          new User(
              existingId,
              "Previous",
              new Email("previous@mail.com"),
              PasswordHash.generate(VALID_PASSWORD, env.BCRYPT_LOG_ROUNDS()),
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
      LocalDateTime currentDate = LocalDateTime.now();
      User existingUser =
          new User(
              existingId,
              "Previous",
              new Email("previous@mail.com"),
              PasswordHash.generate(VALID_PASSWORD, env.BCRYPT_LOG_ROUNDS()),
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
      LocalDateTime currentDate = LocalDateTime.now();
      User existingUser =
          new User(
              existingId,
              "Previous",
              new Email("previous@email.com"),
              PasswordHash.generate(VALID_PASSWORD, env.BCRYPT_LOG_ROUNDS()),
              currentDate,
              currentDate);

      User expectedUpdatedUser =
          existingUser.withPasswordHash(
              PasswordHash.generate(payload.password(), env.BCRYPT_LOG_ROUNDS()));

      when(userRepository.findById(existingId)).thenReturn(Optional.of(existingUser));
      when(userRepository.save(
              argThat(
                  user ->
                      user.getId() == existingId
                          && user.getEmail().equals(expectedUpdatedUser.getEmail())
                          && user.getPasswordHash().matches(payload.password())
                          && user.getUpdatedAt().isEqual(expectedUpdatedUser.getUpdatedAt())
                          && user.getCreatedAt().isEqual(expectedUpdatedUser.getCreatedAt()))))
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
      LocalDateTime now = LocalDateTime.now();
      User existingUser =
          new User(
              updatingId,
              "Previous",
              new Email(payload.email()),
              PasswordHash.generate(VALID_PASSWORD, env.BCRYPT_LOG_ROUNDS()),
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
      LocalDateTime currentDate = LocalDateTime.now();
      User existingUser =
          new User(
              existingId,
              "Previous",
              new Email("previous@email.com"),
              PasswordHash.generate(VALID_PASSWORD, env.BCRYPT_LOG_ROUNDS()),
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
  class FailureScenarios {

    @Test
    @DisplayName("getUserById: given a non-existing id, then throw EntityNotFoundException")
    public void getUserById_nonExistingId_throwEntityNotFoundException() {
      int nonExistingId = 999;
      when(userRepository.findById(nonExistingId)).thenReturn(Optional.empty());

      assertThatThrownBy(() -> userService.getUserById(nonExistingId))
          .isInstanceOf(EntityNotFoundException.class)
          .hasMessage("User with id " + nonExistingId + " not found");
    }

    @Test
    @DisplayName("createUser: given an existing email, then throw IllegalArgumentException")
    public void createUser_existingEmail_throwIllegalArgumentException() {
      LocalDateTime currentDate = LocalDateTime.now();
      UserUpdatePayload payload = createPayload("User 1", "user1@mail.com", VALID_PASSWORD);
      User existingUser =
          new User(
              1,
              payload.name(),
              new Email(payload.email()),
              PasswordHash.generate(payload.password(), env.BCRYPT_LOG_ROUNDS()),
              currentDate,
              currentDate);
      when(userRepository.findByEmail(new Email(payload.email())))
          .thenReturn(Optional.of(existingUser));

      assertThatThrownBy(() -> userService.createUser(payload))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessage("Email already in use. Please use another");
    }

    @ParameterizedTest
    @CsvSource({"blank", "format"})
    @DisplayName("createUser: given an invalid email format, then throw IllegalArgumentException")
    public void createUser_invalidEmailFormat_throwIllegalArgumentException(
        String invalidationCause) {
      String email = invalidationCause.equals("format") ? "mailemail.com" : " ";
      UserUpdatePayload payload = createPayload("User", email, VALID_PASSWORD);

      assertThatThrownBy(() -> userService.createUser(payload))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessage("Invalid email format: " + payload.email());
    }

    @ParameterizedTest
    @CsvSource({"blank", "format"})
    @DisplayName("createUser: given an invalid password, then throw IllegalArgumentException")
    public void createUser_invalidPassword_throwIllegalArgumentException(String invalidationCause) {
      String password = invalidationCause.equals("format") ? "invalid" : " ";
      UserUpdatePayload payload = createPayload("User", "mail@email.com", password);
      when(userRepository.findByEmail(new Email(payload.email()))).thenReturn(Optional.empty());

      assertThatThrownBy(() -> userService.createUser(payload))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessage("Password must be between 8 and 72 characters.");
    }

    @Test
    @DisplayName("updateUserById: given a non-existing id, then throw EntityNotFoundException")
    public void updateUserById_nonExistingId_throwEntityNotFoundException() {
      int nonExistingId = 999;
      UserUpdatePayload payload = createPayload("New Name", null, null);

      when(userRepository.findById(nonExistingId)).thenReturn(Optional.empty());

      assertThatThrownBy(() -> userService.updateUserById(nonExistingId, payload))
          .isInstanceOf(EntityNotFoundException.class)
          .hasMessage("User not found");
    }

    @ParameterizedTest
    @CsvSource({"blank", "format"})
    @DisplayName(
        "updateUserById: given an invalid email format, then throw IllegalArgumentException")
    public void updateUserById_invalidEmailFormat_throwIllegalArgumentException(
        String invalidationCause) {
      int existingId = 1;
      String email = invalidationCause.equals("format") ? "invalidemail" : " ";
      UserUpdatePayload payload = createPayload(null, email, null);
      LocalDateTime currentDate = LocalDateTime.now();
      User existingUser =
          new User(
              existingId,
              "Previous",
              new Email("previous@mail.com"),
              PasswordHash.generate(VALID_PASSWORD, env.BCRYPT_LOG_ROUNDS()),
              currentDate,
              currentDate);

      when(userRepository.findById(existingId)).thenReturn(Optional.of(existingUser));

      assertThatThrownBy(() -> userService.updateUserById(existingId, payload))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessage("Invalid email format: " + payload.email());
    }

    @ParameterizedTest
    @CsvSource({"blank", "format"})
    @DisplayName("updateUserById: given an invalid password, then throw IllegalArgumentException")
    public void updateUserById_invalidPasswordFormat_throwIllegalArgumentException(
        String invalidationCause) {
      int existingId = 1;
      String password = invalidationCause.equals("format") ? "invalid" : " ";
      UserUpdatePayload payload = createPayload(null, null, password);
      LocalDateTime currentDate = LocalDateTime.now();
      User existingUser =
          new User(
              existingId,
              "Previous",
              new Email("previous@mail.com"),
              PasswordHash.generate(VALID_PASSWORD, env.BCRYPT_LOG_ROUNDS()),
              currentDate,
              currentDate);

      when(userRepository.findById(existingId)).thenReturn(Optional.of(existingUser));

      assertThatThrownBy(() -> userService.updateUserById(existingId, payload))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessage("Password must be between 8 and 72 characters.");
    }

    @Test
    @DisplayName(
        "updateUserById: given an email already in use by another user, then throw IllegalArgumentException")
    public void updateUserById_emailAlreadyUsedByAnotherUser_throwIllegalArgumentException() {
      int updatingId = 1;
      UserUpdatePayload payload = createPayload(null, "updating@mail.com", null);
      LocalDateTime now = LocalDateTime.now();
      User existingUser =
          new User(
              2,
              "Previous",
              new Email("previous@mail.com"),
              PasswordHash.generate(VALID_PASSWORD, env.BCRYPT_LOG_ROUNDS()),
              now,
              now);

      when(userRepository.findById(updatingId)).thenReturn(Optional.of(existingUser));
      when(userRepository.findByEmail(new Email(payload.email())))
          .thenReturn(Optional.of(existingUser));

      assertThatThrownBy(() -> userService.updateUserById(updatingId, payload))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessage("Email already in use. Please use another");
    }

    @Test
    @DisplayName("deleteUserById: given a non-existing id, then throw EntityNotFoundException")
    public void deleteUserById_nonExistingId_throwEntityNotFoundException() {
      int nonExistingId = 999;
      when(userRepository.deleteById(nonExistingId)).thenReturn(false);

      assertThatThrownBy(() -> userService.deleteUserById(nonExistingId))
          .isInstanceOf(EntityNotFoundException.class)
          .hasMessage("User with id " + nonExistingId + " not found");
    }
  }
}
