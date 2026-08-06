package com.anibalxyz.features.users.api;

import static com.anibalxyz.shared.Constants.Users.*;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

import com.anibalxyz.core.Result;
import com.anibalxyz.core.application.ValidationNotification;
import com.anibalxyz.core.application.exception.FailureSignal;
import com.anibalxyz.features.common.api.out.response.success.CollectionResponse;
import com.anibalxyz.features.users.api.in.UserCreateRequest;
import com.anibalxyz.features.users.api.in.UserUpdateRequest;
import com.anibalxyz.features.users.application.CreateUser;
import com.anibalxyz.features.users.application.DeleteUserById;
import com.anibalxyz.features.users.application.GetAllUsers;
import com.anibalxyz.features.users.application.GetUserById;
import com.anibalxyz.features.users.application.UpdateUserById;
import com.anibalxyz.features.users.application.in.CreateUserCommand;
import com.anibalxyz.features.users.application.in.UpdateUserCommand;
import com.anibalxyz.features.users.domain.User;
import com.anibalxyz.features.users.domain.error.UserNotFoundError;
import com.anibalxyz.shared.Constants;
import io.javalin.http.BadRequestResponse;
import io.javalin.http.Context;
import io.javalin.validation.Validator;
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
  @Mock private GetAllUsers getAllUsers;

  @Mock private GetUserById getUserById;

  @Mock private CreateUser createUser;

  @Mock private UpdateUserById updateUserById;

  @Mock private DeleteUserById deleteUserById;

  @Mock private Context ctx;

  @InjectMocks private UserController userController;

  @BeforeAll
  public static void setup() {
    Constants.init();
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
      when(getUserById.execute(nonExistingId))
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
      when(createUser.execute(command)).thenReturn(Result.failure(new ValidationNotification<>()));

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
    @DisplayName("updateUserById: given the service returns Error, then throw FailureSignal")
    public void updateUserById_serviceReturnsUpdateUserByIdError_throwFailureSignal() {
      UserUpdateRequest request = mock(UserUpdateRequest.class);
      UpdateUserCommand command = mock(UpdateUserCommand.class);
      when(request.toCommand()).thenReturn(command);
      whenGettingPathParamId().thenReturn(1);

      when(ctx.bodyAsClass(UserUpdateRequest.class)).thenReturn(request);
      when(updateUserById.execute(1, command))
          .thenReturn(Result.failure(new UpdateUserById.Error.EmptyCommand()));

      assertThatThrownBy(() -> userController.updateUserById(ctx))
          .isInstanceOf(FailureSignal.class)
          .extracting(fs -> ((FailureSignal) fs).getError())
          .isInstanceOf(UpdateUserById.Error.class);
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
      when(deleteUserById.execute(nonExistingId))
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
      List<User> fakeUsers = List.of(buildUser(1), buildUser(2));

      when(getAllUsers.execute()).thenReturn(fakeUsers);
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

      when(getAllUsers.execute()).thenReturn(fakeUsers);
      userController.getAllUsers(ctx);

      verify(ctx).status(200);
      verify(ctx).json(CollectionResponse.ofSinglePage(List.of()));
    }

    @Test
    @DisplayName("getUserById: given the service returns User, then return 200 with User")
    public void getUserById_serviceReturnsUser_respond200WithUser() {
      User fakeUser = VALID_USER;

      whenGettingPathParamId().thenReturn(fakeUser.id());
      when(getUserById.execute(fakeUser.id())).thenReturn(Result.success(fakeUser));

      userController.getUserById(ctx);

      verify(ctx).status(200);
      verify(ctx).json(UserMapper.toDetailResponse(fakeUser));
    }

    @Test
    @DisplayName("createUser: given the service returns User, then respond 201 with new user")
    public void createUser_serviceReturnsUser_respond201WithNewUser() {
      User user = VALID_USER;
      UserCreateRequest request =
          new UserCreateRequest(user.name().value(), user.email().value(), VALID_PASSWORD_STRING);

      when(ctx.bodyAsClass(UserCreateRequest.class)).thenReturn(request);

      when(createUser.execute(request.toCommand())).thenReturn(Result.success(user));

      userController.createUser(ctx);

      verify(ctx).status(201);
      verify(ctx).json(UserMapper.toCreateResponse(user));
    }

    @Test
    @DisplayName(
        "updateUserById: given the service returns User, then respond 200 with updated user")
    public void updateUserById_serviceReturnsUser_respond200WithUpdatedUser() {
      UserUpdateRequest request =
          new UserUpdateRequest(VALID_EMAIL_STRING, VALID_EMAIL_STRING, VALID_PASSWORD_STRING);

      User fakeUser = VALID_USER;

      whenGettingPathParamId().thenReturn(fakeUser.id());
      when(ctx.bodyAsClass(UserUpdateRequest.class)).thenReturn(request);
      when(updateUserById.execute(fakeUser.id(), request.toCommand()))
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
      when(deleteUserById.execute(validId)).thenReturn(Result.success(null));

      userController.deleteUserById(ctx);

      verify(ctx).status(204);
      verify(ctx, never()).json(any());
    }
  }
}
