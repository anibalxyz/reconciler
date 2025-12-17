package com.anibalxyz.features.users.api;

import static com.anibalxyz.features.Constants.Environment.BCRYPT_LOG_ROUNDS;
import static com.anibalxyz.features.Helpers.capturedJsonAs;
import static com.anibalxyz.features.Helpers.stubBodyValidatorFor;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

import com.anibalxyz.features.Constants;
import com.anibalxyz.features.common.application.exception.InvalidInputException;
import com.anibalxyz.features.common.application.exception.ResourceNotFoundException;
import com.anibalxyz.features.users.api.in.UserCreateRequest;
import com.anibalxyz.features.users.api.in.UserUpdateRequest;
import com.anibalxyz.features.users.api.out.UserCreateResponse;
import com.anibalxyz.features.users.api.out.UserDetailResponse;
import com.anibalxyz.features.users.application.UserService;
import com.anibalxyz.features.users.domain.Email;
import com.anibalxyz.features.users.domain.PasswordHash;
import com.anibalxyz.features.users.domain.User;
import io.javalin.http.BadRequestResponse;
import io.javalin.http.Context;
import io.javalin.validation.ValidationError;
import io.javalin.validation.ValidationException;
import io.javalin.validation.Validator;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.OngoingStubbing;

@ExtendWith(MockitoExtension.class)
@DisplayName("Tests for UserController")
public class UserControllerTest {

  @Mock private UserService userService;

  @Mock private Context ctx;

  @InjectMocks private UserController userController;

  @BeforeAll
  public static void setup() {
    Constants.init();
  }

  @SuppressWarnings("unchecked")
  private OngoingStubbing<Integer> stubPathParamId() {
    Validator<Integer> mockValidator = (Validator<Integer>) mock(Validator.class);
    when(ctx.pathParamAsClass("id", Integer.class)).thenReturn(mockValidator);
    return when(mockValidator.getOrThrow(any()));
  }

  @Nested
  @DisplayName("Failure Scenarios")
  class FailureScenarios {

    @Test
    @DisplayName("getUserById: given a non-existing id, then throw ResourceNotFoundException")
    public void getUserById_nonExistingId_throwsResourceNotFoundException() {
      int nonExistingId = 999;
      stubPathParamId().thenReturn(nonExistingId);
      when(userService.getUserById(nonExistingId)).thenThrow(new ResourceNotFoundException(""));

      assertThatThrownBy(() -> userController.getUserById(ctx))
          .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("getUserById: given an invalid id, then throw BadRequestResponse")
    public void getUserById_invalidId_throwsBadRequestResponse() {
      stubPathParamId().thenThrow(new BadRequestResponse());

      assertThatThrownBy(() -> userController.getUserById(ctx))
          .isInstanceOf(BadRequestResponse.class);
    }

    @Test
    @DisplayName("createUser: given an invalid property, then throw InvalidInputException")
    public void createUser_invalidProperty_throwsInvalidInputException() {
      UserCreateRequest request = new UserCreateRequest("John Doe", "mail.com", "abc");
      stubBodyValidatorFor(ctx, UserCreateRequest.class).thenReturn(request);

      when(userService.createUser(request)).thenThrow(new InvalidInputException(""));

      assertThatThrownBy(() -> userController.createUser(ctx))
          .isInstanceOf(InvalidInputException.class);
    }

    @Test
    @DisplayName("createUser: given a missing property, then throw ValidationException")
    public void createUser_missingProperty_throwsValidationException() {
      stubBodyValidatorFor(ctx, UserCreateRequest.class)
          .thenThrow(
              new ValidationException(
                  Map.of("property", List.of(new ValidationError<>("Property is required")))));

      assertThatThrownBy(() -> userController.createUser(ctx))
          .isInstanceOf(ValidationException.class);
    }

    @Test
    @DisplayName("updateUserById: given an invalid id, then throw BadRequestResponse")
    public void updateUserById_invalidId_throwsBadRequestResponse() {
      stubPathParamId().thenThrow(new BadRequestResponse());
      assertThatThrownBy(() -> userController.updateUserById(ctx))
          .isInstanceOf(BadRequestResponse.class);
    }

    @Test
    @DisplayName("updateUserById: given missing data, then throw ValidationException")
    public void updateUserById_missingData_throwsValidationException() {
      stubPathParamId().thenReturn(1);
      stubBodyValidatorFor(ctx, UserUpdateRequest.class)
          .thenThrow(
              new ValidationException(
                  Map.of("property", List.of(new ValidationError<>("Property is required")))));
      assertThatThrownBy(() -> userController.updateUserById(ctx))
          .isInstanceOf(ValidationException.class);
    }

    @Test
    @DisplayName("updateUserById: given an invalid property, then throw InvalidInputException")
    public void updateUserById_invalidProperty_throwsInvalidInputException() {
      UserUpdateRequest request = new UserUpdateRequest("John Doe", "mail.com", "abc");
      int validId = 1;

      stubPathParamId().thenReturn(validId);
      stubBodyValidatorFor(ctx, UserUpdateRequest.class).thenReturn(request);

      when(userService.updateUserById(validId, request)).thenThrow(new InvalidInputException(""));

      assertThatThrownBy(() -> userController.updateUserById(ctx))
          .isInstanceOf(InvalidInputException.class);
    }

    @Test
    @DisplayName("updateUserById: given a non-existing id, then throw ResourceNotFoundException")
    public void updateUserById_nonExistingId_throwsResourceNotFoundException() {
      UserUpdateRequest request = new UserUpdateRequest("John Doe", "mail.com", "abc");
      int nonExistingId = 999;
      stubPathParamId().thenReturn(nonExistingId);
      stubBodyValidatorFor(ctx, UserUpdateRequest.class).thenReturn(request);

      when(userService.updateUserById(nonExistingId, request))
          .thenThrow(new ResourceNotFoundException(""));

      assertThatThrownBy(() -> userController.updateUserById(ctx))
          .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("deleteUserById: given an invalid id, then throw BadRequestResponse")
    public void deleteUserById_invalidId_throwsBadRequestResponse() {
      stubPathParamId().thenThrow(new BadRequestResponse());
      assertThatThrownBy(() -> userController.deleteUserById(ctx))
          .isInstanceOf(BadRequestResponse.class);
    }

    @Test
    @DisplayName("deleteUserById: given a non-existing id, then throw ResourceNotFoundException")
    public void deleteUserById_nonExistingId_throwsResourceNotFoundException() {
      int nonExistingId = 999;
      stubPathParamId().thenReturn(nonExistingId);
      doThrow(new ResourceNotFoundException("")).when(userService).deleteUserById(nonExistingId);
      assertThatThrownBy(() -> userController.deleteUserById(ctx))
          .isInstanceOf(ResourceNotFoundException.class);
    }
  }

  @Nested
  @DisplayName("Success Scenarios")
  class SuccessScenarios {
    @BeforeEach
    void setUp() {
      when(ctx.status(anyInt())).thenReturn(ctx);
    }

    @Test
    @DisplayName("getAllUsers: given users exist, then return 200 and users as JSON")
    public void getAllUsers_return200AndUsersJson() {
      Instant instant = Instant.now();
      int logRounds = BCRYPT_LOG_ROUNDS;
      // Given there are users
      List<User> fakeUsers =
          List.of(
              new User(
                  1,
                  "John Doe",
                  new Email("john.doe@example.com"),
                  PasswordHash.generate("12345678", logRounds),
                  instant,
                  instant),
              new User(
                  2,
                  "Jane Smith",
                  new Email("jane.smith@example.com"),
                  PasswordHash.generate("87654321", logRounds),
                  instant,
                  instant));

      // When getAllUsers is called
      when(userService.getAllUsers()).thenReturn(fakeUsers);
      userController.getAllUsers(ctx);

      verify(ctx).status(200);

      @SuppressWarnings("unchecked")
      List<UserDetailResponse> actual = capturedJsonAs(ctx, List.class);

      List<UserDetailResponse> expected =
          fakeUsers.stream().map(UserMapper::toDetailResponse).toList();

      // then return users as JSON
      assertThat(actual).isEqualTo(expected);
    }

    @Test
    @DisplayName("getAllUsers: given no users exist, then return 200 and empty JSON array")
    public void getAllUsers_return200AndEmptyJsonArray() {
      List<User> fakeUsers = List.of();

      when(userService.getAllUsers()).thenReturn(fakeUsers);
      userController.getAllUsers(ctx);

      verify(ctx).status(200);

      @SuppressWarnings("unchecked")
      List<UserDetailResponse> actual = capturedJsonAs(ctx, List.class);
      List<UserDetailResponse> expected = List.of();

      assertThat(actual).isEqualTo(expected);
    }

    @Test
    @DisplayName("getUserById: given an existing id, then return 200 and user as JSON")
    public void getUserById_existingId_returns200AndUserJson() {
      Instant instant = Instant.now();
      int logRounds = BCRYPT_LOG_ROUNDS;
      int id = 1;
      User fakeUser =
          new User(
              id,
              "John Doe",
              new Email("johndoe@gmail.com"),
              PasswordHash.generate("12345678", logRounds),
              instant,
              instant);

      stubPathParamId().thenReturn(id);
      when(userService.getUserById(id)).thenReturn(fakeUser);

      userController.getUserById(ctx);

      verify(ctx).status(200);

      UserDetailResponse actual = capturedJsonAs(ctx, UserDetailResponse.class);
      UserDetailResponse expected = UserMapper.toDetailResponse(fakeUser);

      assertThat(actual).isEqualTo(expected);
    }

    @Test
    @DisplayName("createUser: given valid data, then return 201 and new user")
    public void createUser_validData_return201AndNewUser() {
      Instant instant = Instant.now();
      int logRounds = BCRYPT_LOG_ROUNDS;
      UserCreateRequest request =
          new UserCreateRequest("John Doe", "johndoe@gmail.com", "12345678");
      User fakeUser =
          new User(
              1,
              request.name(),
              new Email(request.email()),
              PasswordHash.generate(request.password(), logRounds),
              instant,
              instant);

      stubBodyValidatorFor(ctx, UserCreateRequest.class).thenReturn(request);

      when(userService.createUser(request)).thenReturn(fakeUser);

      userController.createUser(ctx);

      verify(ctx).status(201);

      UserCreateResponse actualResponse = capturedJsonAs(ctx, UserCreateResponse.class);
      UserCreateResponse expectedResponse = UserMapper.toCreateResponse(fakeUser);

      assertThat(actualResponse).isEqualTo(expectedResponse);
    }

    @Test
    @DisplayName(
        "updateUserById: given an existing id and valid data, then return 200 and updated user")
    public void updateUserById_existingIdAndValidData_returns200AndUpdatedUser() {
      UserUpdateRequest request =
          new UserUpdateRequest("John Doe", "john@mail.com", "password12345678");
      int id = 1;

      Instant instant = Instant.now();
      int logRounds = BCRYPT_LOG_ROUNDS;
      User fakeUser =
          new User(
              id,
              request.name(),
              new Email(request.email()),
              PasswordHash.generate(request.password(), logRounds),
              instant,
              instant);

      stubPathParamId().thenReturn(id);
      stubBodyValidatorFor(ctx, UserUpdateRequest.class).thenReturn(request);
      when(userService.updateUserById(id, request)).thenReturn(fakeUser);

      userController.updateUserById(ctx);

      verify(ctx).status(200);

      UserDetailResponse expectedResponse = UserMapper.toDetailResponse(fakeUser);
      UserDetailResponse actualResponse = capturedJsonAs(ctx, UserDetailResponse.class);

      assertThat(actualResponse).isEqualTo(expectedResponse);
    }

    @Test
    @DisplayName("deleteUserById: given an existing id, then return 204 No Content")
    public void deleteUserById_existingId_returns204NoContent() {
      int validId = 1;
      stubPathParamId().thenReturn(validId);
      doNothing().when(userService).deleteUserById(validId);

      userController.deleteUserById(ctx);

      verify(ctx).status(204);
      verify(ctx, never()).json(any());
    }
  }
}
