package com.anibalxyz.features.users.api;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

import com.anibalxyz.core.Result;
import com.anibalxyz.core.application.ValidationNotification;
import com.anibalxyz.core.application.exception.FailureSignal;
import com.anibalxyz.features.common.api.out.response.success.CollectionResponse;
import com.anibalxyz.features.users.api.in.UserCreateRequest;
import com.anibalxyz.features.users.api.in.UserUpdateRequest;
import com.anibalxyz.features.users.application.UserService;
import com.anibalxyz.features.users.application.in.CreateUserCommand;
import com.anibalxyz.features.users.application.in.UpdateUserCommand;
import com.anibalxyz.features.users.domain.Email;
import com.anibalxyz.features.users.domain.Name;
import com.anibalxyz.features.users.domain.PasswordHash;
import com.anibalxyz.features.users.domain.User;
import com.anibalxyz.features.users.domain.error.UserNotFoundError;
import com.anibalxyz.shared.Constants;
import com.anibalxyz.shared.ResultAsserts;
import io.javalin.http.BadRequestResponse;
import io.javalin.http.Context;
import io.javalin.validation.Validator;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.OngoingStubbing;

@ExtendWith(MockitoExtension.class)
@DisplayName("Tests for UserController")
public class UserControllerTest {

  private static int BCRYPT_LOG_ROUNDS;

  @Mock private UserService userService;

  @Mock private Context ctx;

  @InjectMocks private UserController userController;

  @BeforeAll
  public static void setup() {
    Constants.init();
    BCRYPT_LOG_ROUNDS = Constants.APP_ENV.BCRYPT_LOG_ROUNDS();
  }

  @SuppressWarnings("unchecked")
  private OngoingStubbing<Integer> whenGettingPathParamId() {
    Validator<Integer> mockValidator = (Validator<Integer>) mock(Validator.class);
    when(ctx.pathParamAsClass("id", Integer.class)).thenReturn(mockValidator);
    return when(mockValidator.getOrThrow(any()));
  }

  @Nested
  @DisplayName("Failure Scenarios")
  class FailureScenarios {
    @Test
    @DisplayName(
        "getUserById: given the service returns UserNotFoundError, then throw FailureSignal")
    public void getUserById_serviceReturnsUserNotFoundError_throwFailureSignal() {
      int nonExistingId = 999;
      whenGettingPathParamId().thenReturn(nonExistingId);
      when(userService.getUserById(nonExistingId))
          .thenReturn(Result.failure(UserNotFoundError.byId(nonExistingId)));

      assertThatThrownBy(() -> userController.getUserById(ctx))
          .isInstanceOf(FailureSignal.class)
          .extracting(e -> ((FailureSignal) e).getError())
          .isInstanceOf(UserNotFoundError.class);
    }

    @Test
    @DisplayName("getUserById: given an invalid id, then throw BadRequestResponse")
    public void getUserById_invalidId_throwBadRequestResponse() {
      whenGettingPathParamId().thenThrow(new BadRequestResponse());

      assertThatThrownBy(() -> userController.getUserById(ctx))
          .isInstanceOf(BadRequestResponse.class);
    }

    @Test
    @DisplayName(
        "createUser: given the service returns ValidationNotification, then throw FailureSignal")
    public void createUser_serviceReturnsValidationNotification_throwFailureSignal() {
      UserCreateRequest request = mock(UserCreateRequest.class);
      CreateUserCommand command = mock(CreateUserCommand.class);
      when(request.toCommand()).thenReturn(command);

      when(ctx.bodyAsClass(UserCreateRequest.class)).thenReturn(request);
      when(userService.createUser(command))
          .thenReturn(Result.failure(new ValidationNotification<>()));

      assertThatThrownBy(() -> userController.createUser(ctx))
          .isInstanceOf(FailureSignal.class)
          .extracting(fs -> ((FailureSignal) fs).getError())
          .isInstanceOf(ValidationNotification.class);
    }

    @Test
    @DisplayName("updateUserById: given an invalid id, then throw BadRequestResponse")
    public void updateUserById_invalidId_throwBadRequestResponse() {
      whenGettingPathParamId().thenThrow(new BadRequestResponse());

      assertThatThrownBy(() -> userController.updateUserById(ctx))
          .isInstanceOf(BadRequestResponse.class);
    }

    @Test
    @DisplayName(
        "updateUserById: given the service returns UpdateUserByIdError, then throw FailureSignal")
    public void updateUserById_serviceReturnsUpdateUserByIdError_throwFailureSignal() {
      UserUpdateRequest request = mock(UserUpdateRequest.class);
      UpdateUserCommand command = mock(UpdateUserCommand.class);
      when(request.toCommand()).thenReturn(command);
      whenGettingPathParamId().thenReturn(1);

      when(ctx.bodyAsClass(UserUpdateRequest.class)).thenReturn(request);
      when(userService.updateUserById(1, command))
          .thenReturn(Result.failure(new UserService.UpdateUserByIdError.EmptyCommand()));

      assertThatThrownBy(() -> userController.updateUserById(ctx))
          .isInstanceOf(FailureSignal.class)
          .extracting(fs -> ((FailureSignal) fs).getError())
          .isInstanceOf(UserService.UpdateUserByIdError.class);
    }

    @Test
    @DisplayName("deleteUserById: given an invalid id, then throw BadRequestResponse")
    public void deleteUserById_invalidId_throwBadRequestResponse() {
      whenGettingPathParamId().thenThrow(new BadRequestResponse());

      assertThatThrownBy(() -> userController.deleteUserById(ctx))
          .isInstanceOf(BadRequestResponse.class);
    }

    @Test
    @DisplayName(
        "deleteUserById: given the service returns UserNotFoundError, then throw FailureSignal")
    public void deleteUserById_serviceReturnsUserNotFoundError_throwFailureSignal() {
      int nonExistingId = 999;
      whenGettingPathParamId().thenReturn(nonExistingId);
      when(userService.deleteUserById(nonExistingId))
          .thenReturn(Result.failure(UserNotFoundError.byId(nonExistingId)));

      assertThatThrownBy(() -> userController.deleteUserById(ctx))
          .isInstanceOf(FailureSignal.class)
          .extracting(fs -> ((FailureSignal) fs).getError())
          .isInstanceOf(UserNotFoundError.class);
    }
  }

  @Nested
  @DisplayName("Success Scenarios")
  class SuccessScenarios {
    @BeforeEach
    public void stubStatusChaining() {
      when(ctx.status(anyInt())).thenReturn(ctx);
    }

    @Test
    @DisplayName("getAllUsers: given there are users, then respond 200 with users list")
    public void getAllUsers_thereAreUsers_respond200WithUsersList() {
      Instant instant = Instant.now();
      List<User> fakeUsers =
          List.of(
              User.reconstitute(
                  1,
                  ResultAsserts.success(Name.of("John Doe")),
                  ResultAsserts.success(Email.of("john.doe@example.com")),
                  ResultAsserts.success(PasswordHash.generate("12345678", BCRYPT_LOG_ROUNDS)),
                  instant,
                  instant),
              User.reconstitute(
                  2,
                  ResultAsserts.success(Name.of("Jane Smith")),
                  ResultAsserts.success(Email.of("jane.smith@example.com")),
                  ResultAsserts.success(PasswordHash.generate("87654321", BCRYPT_LOG_ROUNDS)),
                  instant,
                  instant));

      when(userService.getAllUsers()).thenReturn(fakeUsers);
      userController.getAllUsers(ctx);

      verify(ctx).status(200);
      var expected =
          CollectionResponse.ofSinglePage(
              fakeUsers.stream().map(UserMapper::toDetailResponse).toList());
      verify(ctx).json(expected);
    }

    @Test
    @DisplayName("getAllUsers: given there are no users, then respond 200 with empty list")
    public void getAllUsers_thereAreNoUsers_respond200WithEmptyList() {
      List<User> fakeUsers = List.of();

      when(userService.getAllUsers()).thenReturn(fakeUsers);
      userController.getAllUsers(ctx);

      verify(ctx).status(200);
      verify(ctx).json(CollectionResponse.ofSinglePage(List.of()));
    }

    @Test
    @DisplayName("getUserById: given the service returns User, then return 200 with User")
    public void getUserById_serviceReturnsUser_respond200WithUser() {
      Instant instant = Instant.now();
      int id = 1;
      User fakeUser =
          User.reconstitute(
              id,
              ResultAsserts.success(Name.of("John Doe")),
              ResultAsserts.success(Email.of("johndoe@gmail.com")),
              ResultAsserts.success(PasswordHash.generate("12345678", BCRYPT_LOG_ROUNDS)),
              instant,
              instant);

      whenGettingPathParamId().thenReturn(id);
      when(userService.getUserById(id)).thenReturn(Result.success(fakeUser));

      userController.getUserById(ctx);

      verify(ctx).status(200);
      verify(ctx).json(UserMapper.toDetailResponse(fakeUser));
    }

    @Test
    @DisplayName("createUser: given the service returns User, then respond 201 with new user")
    public void createUser_serviceReturnsUser_respond201WithNewUser() {
      Instant instant = Instant.now();
      UserCreateRequest request =
          new UserCreateRequest("John Doe", "johndoe@gmail.com", "12345678");
      User fakeUser =
          User.reconstitute(
              1,
              ResultAsserts.success(Name.of(request.name())),
              ResultAsserts.success(Email.of(request.email())),
              ResultAsserts.success(PasswordHash.generate(request.password(), BCRYPT_LOG_ROUNDS)),
              instant,
              instant);

      when(ctx.bodyAsClass(UserCreateRequest.class)).thenReturn(request);

      when(userService.createUser(request.toCommand())).thenReturn(Result.success(fakeUser));

      userController.createUser(ctx);

      verify(ctx).status(201);
      verify(ctx).json(UserMapper.toCreateResponse(fakeUser));
    }

    @Test
    @DisplayName(
        "updateUserById: given the service returns User, then respond 200 with updated user")
    public void updateUserById_serviceReturnsUser_respond200WithUpdatedUser() {
      UserUpdateRequest request =
          new UserUpdateRequest("John Doe", "john@mail.com", "password12345678");
      int id = 1;

      Instant instant = Instant.now();
      User fakeUser =
          User.reconstitute(
              id,
              ResultAsserts.success(Name.of(request.name())),
              ResultAsserts.success(Email.of(request.email())),
              ResultAsserts.success(PasswordHash.generate(request.password(), BCRYPT_LOG_ROUNDS)),
              instant,
              instant);

      whenGettingPathParamId().thenReturn(id);
      when(ctx.bodyAsClass(UserUpdateRequest.class)).thenReturn(request);
      when(userService.updateUserById(id, request.toCommand()))
          .thenReturn(Result.success(fakeUser));

      userController.updateUserById(ctx);

      verify(ctx).status(200);
      verify(ctx).json(UserMapper.toDetailResponse(fakeUser));
    }

    @Test
    @DisplayName("deleteUserById: given the service returns success, then respond 204 no content")
    public void deleteUserById_serviceReturnsSuccess_respond204NoContent() {
      int validId = 1;
      whenGettingPathParamId().thenReturn(validId);
      when(userService.deleteUserById(validId)).thenReturn(Result.success(null));

      userController.deleteUserById(ctx);

      verify(ctx).status(204);
      verify(ctx, never()).json(any());
    }
  }
}
