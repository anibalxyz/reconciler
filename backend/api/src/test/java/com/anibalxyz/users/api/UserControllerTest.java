package com.anibalxyz.users.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

import com.anibalxyz.users.api.in.UserCreateRequest;
import com.anibalxyz.users.api.out.UserCreateResponse;
import com.anibalxyz.users.api.out.UserDetailResponse;
import com.anibalxyz.users.application.UserService;
import com.anibalxyz.users.domain.Email;
import com.anibalxyz.users.domain.PasswordHash;
import com.anibalxyz.users.domain.User;
import io.javalin.http.BadRequestResponse;
import io.javalin.http.Context;
import io.javalin.validation.BodyValidator;
import io.javalin.validation.Validator;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class UserControllerTest {

  @Mock private UserService userService;

  @Mock private Context ctx;

  @InjectMocks private UserController userController;

  @Test
  @DisplayName("Given there are users, when getAllUsers is called, then return users as JSON")
  public void getAllUsers_returnUsersJson() {
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

    List<UserDetailResponse> actual = getCapturedValueOf(List.class);

    List<UserDetailResponse> expected =
        fakeUsers.stream().map(UserMapper::toDetailResponse).toList();

    // then return users as JSON
    assertThat(actual).isEqualTo(expected);
  }

  @Test
  @DisplayName("Given there are no users, when getAllUsers is called, then return empty JSON array")
  public void getAllUsers_returnEmptyJsonArray() {
    List<User> fakeUsers = List.of();

    when(userService.getAllUsers()).thenReturn(fakeUsers);
    userController.getAllUsers(ctx);

    List<UserDetailResponse> actual = getCapturedValueOf(List.class);
    List<UserDetailResponse> expected = List.of();

    assertThat(actual).isEqualTo(expected);
  }

  @Test
  @DisplayName("Given a user ID, when getUserById is called, then return user as JSON")
  public void getUserById_userExists_returnUserJson() {
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
    Optional<User> optionalUser = Optional.of(fakeUser);

    when(userService.getUserById(id)).thenReturn(optionalUser);

    mockGetParamId(id);

    userController.getUserById(ctx);

    UserDetailResponse actual = getCapturedValueOf(UserDetailResponse.class);
    UserDetailResponse expected = UserMapper.toDetailResponse(fakeUser);

    assertThat(actual).isEqualTo(expected);
  }

  @Test
  @DisplayName("Given a user ID that does not exist, when getUserById is called, then return 404")
  public void getUserById_userDoesNotExist_return404() {
    int id = 1;

    mockGetParamId(id);

    when(userService.getUserById(id)).thenReturn(Optional.empty());
    when(ctx.status(anyInt())).thenReturn(ctx);

    userController.getUserById(ctx);

    verify(ctx).status(404);

    Map<String, String> actual = getCapturedValueOf(Map.class);
    Map<String, String> expected = Map.of("error", "User with id " + id + " not found");

    assertThat(actual).isEqualTo(expected);
  }

  @Test
  @DisplayName(
      "Given an invalid user ID, when getUserById is called, then throw BadRequestResponse")
  public void getUserById_invalidId_throwsBadRequestResponse() {
    Validator<Integer> mockValidator = mock(Validator.class);
    // The message is not really being tested, just that it throws the exception
    when(mockValidator.getOrThrow(any()))
        .thenThrow(new BadRequestResponse("Invalid ID format. Must be a number."));
    when(ctx.pathParamAsClass("id", Integer.class)).thenReturn(mockValidator);

    // What really tests is that the exception is thrown and not caught
    assertThatThrownBy(() -> userController.getUserById(ctx))
        .isInstanceOf(BadRequestResponse.class);
  }

  @Test
  @DisplayName("Given valid user data, when createUser is called, then return 201 and new user")
  public void createUser_validData_return201AndNewUser() {
    LocalDateTime localDateTime = LocalDateTime.now();
    UserCreateRequest request = new UserCreateRequest("John Doe", "johndoe@gmail.com", "12345678");
    User fakeUser =
        new User(
            1,
            request.name(),
            new Email(request.email()),
            PasswordHash.generate(request.password()),
            localDateTime,
            localDateTime);

    BodyValidator<UserCreateRequest> mockValidator = mock(BodyValidator.class);

    when(ctx.bodyValidator(UserCreateRequest.class)).thenReturn(mockValidator);
    when(mockValidator.check(any(), anyString())).thenReturn(mockValidator);
    when(mockValidator.get()).thenReturn(request);
    when(ctx.status(anyInt())).thenReturn(ctx);

    when(userService.createUser(request.name(), request.email(), request.password()))
        .thenReturn(fakeUser);

    userController.createUser(ctx);

    verify(ctx).status(201);

    UserCreateResponse actualResponse = getCapturedValueOf(UserCreateResponse.class);
    UserCreateResponse expectedResponse = UserMapper.toCreateResponse(fakeUser);

    assertThat(actualResponse).isEqualTo(expectedResponse);
  }

  @Test
  @DisplayName(
      "Given an invalid property, when createUser is called, then throw IllegalArgumentException")
  public void createUser_invalidProperty_throwsIllegalArgumentException() {
    BodyValidator<UserCreateRequest> mockValidator = mock(BodyValidator.class);
    when(ctx.bodyValidator(UserCreateRequest.class)).thenReturn(mockValidator);
    when(mockValidator.check(any(), anyString())).thenReturn(mockValidator);
    // invalid email and password format
    when(mockValidator.get()).thenReturn(new UserCreateRequest("John Doe", "mail.com", "abc"));

    when(userService.createUser(any(), any(), any())).thenThrow(new IllegalArgumentException());

    assertThatThrownBy(() -> userController.createUser(ctx))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  @DisplayName("Given a missing property, when createUser is called, then throw BadRequestResponse")
  public void createUser_missingProperty_throwsBadRequestResponse() {
    BodyValidator<UserCreateRequest> mockValidator = mock(BodyValidator.class);
    when(ctx.bodyValidator(UserCreateRequest.class)).thenReturn(mockValidator);
    when(mockValidator.check(any(), anyString())).thenReturn(mockValidator);
    when(mockValidator.get()).thenThrow(new BadRequestResponse());

    assertThatThrownBy(() -> userController.createUser(ctx)).isInstanceOf(BadRequestResponse.class);
  }

  private void mockGetParamId(int id) {
    Validator<Integer> mockValidator = mock(Validator.class);
    when(mockValidator.getOrThrow(any())).thenReturn(id);
    when(ctx.pathParamAsClass("id", Integer.class)).thenReturn(mockValidator);
  }

  private <T> T getCapturedValueOf(Class<T> clazz) {
    ArgumentCaptor<T> captor = ArgumentCaptor.forClass(clazz);
    verify(ctx).json(captor.capture());
    return captor.getValue();
  }
}
