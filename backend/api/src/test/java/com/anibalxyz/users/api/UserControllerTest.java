package com.anibalxyz.users.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

import com.anibalxyz.users.api.in.UserCreateRequest;
import com.anibalxyz.users.api.in.UserUpdateRequest;
import com.anibalxyz.users.api.out.UserCreateResponse;
import com.anibalxyz.users.api.out.UserDetailResponse;
import com.anibalxyz.users.application.UserService;
import com.anibalxyz.users.application.exception.EntityNotFoundException;
import com.anibalxyz.users.domain.Email;
import com.anibalxyz.users.domain.PasswordHash;
import com.anibalxyz.users.domain.User;
import io.javalin.http.BadRequestResponse;
import io.javalin.http.Context;
import io.javalin.validation.BodyValidator;
import io.javalin.validation.ValidationError;
import io.javalin.validation.ValidationException;
import io.javalin.validation.Validator;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.OngoingStubbing;

@ExtendWith(MockitoExtension.class)
public class UserControllerTest {

  @Mock private UserService userService;

  @Mock private Context ctx;

  @InjectMocks private UserController userController;

  @SuppressWarnings("unchecked")
  private OngoingStubbing<Integer> stubPathParamId() {
    Validator<Integer> mockValidator = (Validator<Integer>) mock(Validator.class);
    when(ctx.pathParamAsClass("id", Integer.class)).thenReturn(mockValidator);
    return when(mockValidator.getOrThrow(any()));
  }

  @SuppressWarnings("unchecked")
  private <T> OngoingStubbing<T> stubBodyValidatorFor(Class<T> clazz) {
    BodyValidator<T> mockValidator = (BodyValidator<T>) mock(BodyValidator.class);
    when(ctx.bodyValidator(clazz)).thenReturn(mockValidator);
    when(mockValidator.check(any(), anyString())).thenReturn(mockValidator);
    return when(mockValidator.get());
  }

  private <T> T capturedJsonAs(Class<T> clazz) {
    ArgumentCaptor<T> captor = ArgumentCaptor.forClass(clazz);
    verify(ctx).json(captor.capture());
    return captor.getValue();
  }

  @Nested
  @DisplayName("Failure Scenarios")
  class FailureScenarios {

    @Test
    @DisplayName(
        "Given a non-existing id, when getUserById is called, then throw EntityNotFoundException")
    public void getUserById_nonExistingId_throwsEntityNotFoundException() {
      int nonExistingId = 999;
      stubPathParamId().thenReturn(nonExistingId);
      when(userService.getUserById(nonExistingId)).thenThrow(new EntityNotFoundException());

      assertThatThrownBy(() -> userController.getUserById(ctx))
          .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    @DisplayName("Given an invalid id, when getUserById is called, then throw BadRequestResponse")
    public void getUserById_invalidId_throwsBadRequestResponse() {
      stubPathParamId().thenThrow(new BadRequestResponse());

      assertThatThrownBy(() -> userController.getUserById(ctx))
          .isInstanceOf(BadRequestResponse.class);
    }

    @Test
    @DisplayName(
        "Given an invalid property, when createUser is called, then throw IllegalArgumentException")
    public void createUser_invalidProperty_throwsIllegalArgumentException() {
      stubBodyValidatorFor(UserCreateRequest.class)
          .thenReturn(new UserCreateRequest("John Doe", "mail.com", "abc"));

      when(userService.createUser(anyString(), anyString(), anyString()))
          .thenThrow(new IllegalArgumentException());

      assertThatThrownBy(() -> userController.createUser(ctx))
          .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName(
        "Given a missing property, when createUser is called, then throw ValidationException")
    public void createUser_missingProperty_throwsValidationException() {
      stubBodyValidatorFor(UserCreateRequest.class)
          .thenThrow(
              new ValidationException(
                  Map.of("property", List.of(new ValidationError<>("Property is required")))));

      assertThatThrownBy(() -> userController.createUser(ctx))
          .isInstanceOf(ValidationException.class);
    }

    @Test
    @DisplayName(
        "Given an invalid id, when updateUserById is called, then throw BadRequestResponse")
    public void updateUserById_invalidId_throwsBadRequestResponse() {
      stubPathParamId().thenThrow(new BadRequestResponse());
      assertThatThrownBy(() -> userController.updateUserById(ctx))
          .isInstanceOf(BadRequestResponse.class);
    }

    @Test
    @DisplayName(
        "Given missing data, when updateUserById is called, then throw ValidationException")
    public void updateUserById_missingData_throwsValidationException() {
      stubPathParamId().thenReturn(1);
      stubBodyValidatorFor(UserUpdateRequest.class)
          .thenThrow(
              new ValidationException(
                  Map.of("property", List.of(new ValidationError<>("Property is required")))));
      assertThatThrownBy(() -> userController.updateUserById(ctx))
          .isInstanceOf(ValidationException.class);
    }

    @Test
    @DisplayName(
        "Given an invalid property, when updateUserById is called, then throw IllegalArgumentException")
    public void updateUserById_invalidProperty_throwsIllegalArgumentException() {
      UserUpdateRequest request = new UserUpdateRequest("John Doe", "mail.com", "abc");
      int validId = 1;

      stubPathParamId().thenReturn(validId);
      stubBodyValidatorFor(UserUpdateRequest.class).thenReturn(request);

      when(userService.updateUserById(validId, request)).thenThrow(new IllegalArgumentException());

      assertThatThrownBy(() -> userController.updateUserById(ctx))
          .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName(
        "Given a non-existing id, when updateUserById is called, then throw EntityNotFoundException")
    public void updateUserById_nonExistingId_throwsEntityNotFoundException() {
      UserUpdateRequest request = new UserUpdateRequest("John Doe", "mail.com", "abc");
      int nonExistingId = 999;
      stubPathParamId().thenReturn(nonExistingId);
      stubBodyValidatorFor(UserUpdateRequest.class).thenReturn(request);

      when(userService.updateUserById(nonExistingId, request))
          .thenThrow(new EntityNotFoundException());

      assertThatThrownBy(() -> userController.updateUserById(ctx))
          .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    @DisplayName(
        "Given an invalid id, when deleteUserById is called, then throw BadRequestResponse")
    public void deleteUserById_invalidId_throwsBadRequestResponse() {
      stubPathParamId().thenThrow(new BadRequestResponse());
      assertThatThrownBy(() -> userController.deleteUserById(ctx))
          .isInstanceOf(BadRequestResponse.class);
    }

    @Test
    @DisplayName(
        "Given a non-existing id, when deleteUserById is called, then throw EntityNotFoundException")
    public void deleteUserById_nonExistingId_throwsEntityNotFoundException() {
      int nonExistingId = 999;
      stubPathParamId().thenReturn(nonExistingId);
      doThrow(new EntityNotFoundException()).when(userService).deleteUserById(nonExistingId);
      assertThatThrownBy(() -> userController.deleteUserById(ctx))
          .isInstanceOf(EntityNotFoundException.class);
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
    @DisplayName(
        "Given there are users, when getAllUsers is called, then return 200 and users as JSON")
    public void getAllUsers_return200AndUsersJson() {
      LocalDateTime localDateTime = LocalDateTime.now();
      // Given there are users
      List<User> fakeUsers =
          List.of(
              new User(
                  1,
                  "John Doe",
                  new Email("john.doe@example.com"),
                  PasswordHash.generate("12345678"),
                  localDateTime,
                  localDateTime),
              new User(
                  2,
                  "Jane Smith",
                  new Email("jane.smith@example.com"),
                  PasswordHash.generate("87654321"),
                  localDateTime,
                  localDateTime));

      // When getAllUsers is called
      when(userService.getAllUsers()).thenReturn(fakeUsers);
      userController.getAllUsers(ctx);

      verify(ctx).status(200);

      @SuppressWarnings("unchecked")
      List<UserDetailResponse> actual = capturedJsonAs(List.class);

      List<UserDetailResponse> expected =
          fakeUsers.stream().map(UserMapper::toDetailResponse).toList();

      // then return users as JSON
      assertThat(actual).isEqualTo(expected);
    }

    @Test
    @DisplayName(
        "Given there are no users, when getAllUsers is called, then return 200 and empty JSON array")
    public void getAllUsers_return200AndEmptyJsonArray() {
      List<User> fakeUsers = List.of();

      when(userService.getAllUsers()).thenReturn(fakeUsers);
      userController.getAllUsers(ctx);

      verify(ctx).status(200);

      @SuppressWarnings("unchecked")
      List<UserDetailResponse> actual = capturedJsonAs(List.class);
      List<UserDetailResponse> expected = List.of();

      assertThat(actual).isEqualTo(expected);
    }

    @Test
    @DisplayName(
        "Given an existing id, when getUserById is called, then return 200 and user as JSON")
    public void getUserById_existingId_returns200AndUserJson() {
      LocalDateTime localDateTime = LocalDateTime.now();
      int id = 1;
      User fakeUser =
          new User(
              id,
              "John Doe",
              new Email("johndoe@gmail.com"),
              PasswordHash.generate("12345678"),
              localDateTime,
              localDateTime);

      stubPathParamId().thenReturn(id);
      when(userService.getUserById(id)).thenReturn(fakeUser);

      userController.getUserById(ctx);

      verify(ctx).status(200);

      UserDetailResponse actual = capturedJsonAs(UserDetailResponse.class);
      UserDetailResponse expected = UserMapper.toDetailResponse(fakeUser);

      assertThat(actual).isEqualTo(expected);
    }

    @Test
    @DisplayName("Given valid user data, when createUser is called, then return 201 and new user")
    public void createUser_validData_return201AndNewUser() {
      LocalDateTime localDateTime = LocalDateTime.now();
      UserCreateRequest request =
          new UserCreateRequest("John Doe", "johndoe@gmail.com", "12345678");
      User fakeUser =
          new User(
              1,
              request.name(),
              new Email(request.email()),
              PasswordHash.generate(request.password()),
              localDateTime,
              localDateTime);

      stubBodyValidatorFor(UserCreateRequest.class).thenReturn(request);

      when(userService.createUser(request.name(), request.email(), request.password()))
          .thenReturn(fakeUser);

      userController.createUser(ctx);

      verify(ctx).status(201);

      UserCreateResponse actualResponse = capturedJsonAs(UserCreateResponse.class);
      UserCreateResponse expectedResponse = UserMapper.toCreateResponse(fakeUser);

      assertThat(actualResponse).isEqualTo(expectedResponse);
    }

    @Test
    @DisplayName(
        "Given an existing id and valid data, when updateUserById is called, then return 200 and updated user")
    public void updateUserById_existingIdAndValidData_returns200AndUpdatedUser() {
      UserUpdateRequest request =
          new UserUpdateRequest("John Doe", "john@mail.com", "password12345678");
      int id = 1;

      LocalDateTime localDateTime = LocalDateTime.now();
      User fakeUser =
          new User(
              id,
              request.name(),
              new Email(request.email()),
              PasswordHash.generate(request.password()),
              localDateTime,
              localDateTime);

      stubPathParamId().thenReturn(id);
      stubBodyValidatorFor(UserUpdateRequest.class).thenReturn(request);
      when(userService.updateUserById(id, request)).thenReturn(fakeUser);

      userController.updateUserById(ctx);

      verify(ctx).status(200);

      UserDetailResponse expectedResponse = UserMapper.toDetailResponse(fakeUser);
      UserDetailResponse actualResponse = capturedJsonAs(UserDetailResponse.class);

      assertThat(actualResponse).isEqualTo(expectedResponse);
    }

    @Test
    @DisplayName("Given an existing id, when deleteUserById is called, then return 204 No Content")
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
