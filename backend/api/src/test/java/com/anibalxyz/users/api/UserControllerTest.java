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
import io.javalin.validation.Validator;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
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

  @BeforeEach
  public void setUp() {
    // TODO: move non happy-path tests to a separate class to avoid using this in those tests
    lenient().when(ctx.status(anyInt())).thenReturn(ctx);
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
  @DisplayName("Given an existing id, when getUserById is called, then return 200 and user as JSON")
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

    mockParamId(id);
    when(userService.getUserById(id)).thenReturn(fakeUser);

    userController.getUserById(ctx);

    verify(ctx).status(200);

    UserDetailResponse actual = capturedJsonAs(UserDetailResponse.class);
    UserDetailResponse expected = UserMapper.toDetailResponse(fakeUser);

    assertThat(actual).isEqualTo(expected);
  }

  @Test
  @DisplayName(
      "Given a non-existing id, when getUserById is called, then throw EntityNotFoundException")
  public void getUserById_nonExistingId_throwsEntityNotFoundException() {
    int nonExistingId = 999;
    mockParamId(nonExistingId);
    when(userService.getUserById(nonExistingId)).thenThrow(new EntityNotFoundException());

    assertThatThrownBy(() -> userController.getUserById(ctx))
        .isInstanceOf(EntityNotFoundException.class);
  }

  @Test
  @DisplayName("Given an invalid id, when getUserById is called, then throw BadRequestResponse")
  public void getUserById_invalidId_throwsBadRequestResponse() {
    mockParamId(new BadRequestResponse());

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

    mockBodyValidator(UserCreateRequest.class, request);

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
      "Given an invalid property, when createUser is called, then throw IllegalArgumentException")
  public void createUser_invalidProperty_throwsIllegalArgumentException() {
    mockBodyValidator(
        UserCreateRequest.class, new UserCreateRequest("John Doe", "mail.com", "abc"));

    when(userService.createUser(anyString(), anyString(), anyString()))
        .thenThrow(new IllegalArgumentException());

    assertThatThrownBy(() -> userController.createUser(ctx))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  @DisplayName("Given a missing property, when createUser is called, then throw BadRequestResponse")
  public void createUser_missingProperty_throwsBadRequestResponse() {
    mockBodyValidator(UserCreateRequest.class, new BadRequestResponse());

    assertThatThrownBy(() -> userController.createUser(ctx)).isInstanceOf(BadRequestResponse.class);
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

    mockParamId(id);
    mockBodyValidator(UserUpdateRequest.class, request);
    when(userService.updateUserById(id, request)).thenReturn(fakeUser);

    userController.updateUserById(ctx);

    verify(ctx).status(200);

    UserDetailResponse expectedResponse = UserMapper.toDetailResponse(fakeUser);
    UserDetailResponse actualResponse = capturedJsonAs(UserDetailResponse.class);

    assertThat(actualResponse).isEqualTo(expectedResponse);
  }

  @Test
  @DisplayName("Given an invalid id, when updateUserById is called, then throw BadRequestResponse")
  public void updateUserById_invalidId_throwsBadRequestResponse() {
    mockParamId(new BadRequestResponse());
    assertThatThrownBy(() -> userController.updateUserById(ctx))
        .isInstanceOf(BadRequestResponse.class);
  }

  @Test
  @DisplayName("Given missing data, when updateUserById is called, then throw BadRequestResponse")
  public void updateUserById_missingData_throwsBadRequestResponse() {
    mockParamId(1);
    mockBodyValidator(UserUpdateRequest.class, new BadRequestResponse());
    assertThatThrownBy(() -> userController.updateUserById(ctx))
        .isInstanceOf(BadRequestResponse.class);
  }

  @Test
  @DisplayName(
      "Given an invalid property, when updateUserById is called, then throw IllegalArgumentException")
  public void updateUserById_invalidProperty_throwsIllegalArgumentException() {
    UserUpdateRequest request = new UserUpdateRequest("John Doe", "mail.com", "abc");
    int validId = 1;

    mockParamId(validId);
    mockBodyValidator(UserUpdateRequest.class, request);

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
    mockParamId(nonExistingId);
    mockBodyValidator(UserUpdateRequest.class, request);

    when(userService.updateUserById(nonExistingId, request))
        .thenThrow(new EntityNotFoundException());

    assertThatThrownBy(() -> userController.updateUserById(ctx))
        .isInstanceOf(EntityNotFoundException.class);
  }

  @Test
  @DisplayName("Given an existing id, when deleteUserById is called, then return 204 No Content")
  public void deleteUserById_existingId_returns204NoContent() {
    int validId = 1;
    mockParamId(validId);
    doNothing().when(userService).deleteUserById(validId);

    userController.deleteUserById(ctx);

    verify(ctx).status(204);
    verify(ctx, never()).json(any());
  }

  @Test
  @DisplayName("Given an invalid id, when deleteUserById is called, then throw BadRequestResponse")
  public void deleteUserById_invalidId_throwsBadRequestResponse() {
    mockParamId(new BadRequestResponse());
    assertThatThrownBy(() -> userController.deleteUserById(ctx))
        .isInstanceOf(BadRequestResponse.class);
  }

  @Test
  @DisplayName(
      "Given a non-existing id, when deleteUserById is called, then throw EntityNotFoundException")
  public void deleteUserById_nonExistingId_throwsEntityNotFoundException() {
    int nonExistingId = 999;
    mockParamId(nonExistingId);
    doThrow(new EntityNotFoundException()).when(userService).deleteUserById(nonExistingId);
    assertThatThrownBy(() -> userController.deleteUserById(ctx))
        .isInstanceOf(EntityNotFoundException.class);
  }

  /* TODO: refactor thenReturn/thenThrow in mockParamId and mockBodyValidator to reduce
           duplication
  */
  @SuppressWarnings("unchecked")
  private void mockParamId(int validId) {
    Validator<Integer> mockValidator = (Validator<Integer>) mock(Validator.class);
    when(ctx.pathParamAsClass("id", Integer.class)).thenReturn(mockValidator);
    when(mockValidator.getOrThrow(any())).thenReturn(validId);
  }

  @SuppressWarnings("unchecked")
  private void mockParamId(Throwable throwable) {
    Validator<Integer> mockValidator = (Validator<Integer>) mock(Validator.class);
    when(ctx.pathParamAsClass("id", Integer.class)).thenReturn(mockValidator);
    when(mockValidator.getOrThrow(any())).thenThrow(throwable);
  }

  @SuppressWarnings("unchecked")
  private <T> void mockBodyValidator(Class<T> clazz, T request) {
    BodyValidator<T> mockValidator = (BodyValidator<T>) mock(BodyValidator.class);
    when(ctx.bodyValidator(clazz)).thenReturn(mockValidator);
    when(mockValidator.check(any(), anyString())).thenReturn(mockValidator);
    when(mockValidator.get()).thenReturn(request);
  }

  @SuppressWarnings("unchecked")
  private <T> void mockBodyValidator(Class<T> clazz, Throwable throwable) {
    BodyValidator<T> mockValidator = (BodyValidator<T>) mock(BodyValidator.class);
    when(ctx.bodyValidator(clazz)).thenReturn(mockValidator);
    when(mockValidator.check(any(), anyString())).thenReturn(mockValidator);
    when(mockValidator.get()).thenThrow(throwable);
  }

  private <T> T capturedJsonAs(Class<T> clazz) {
    ArgumentCaptor<T> captor = ArgumentCaptor.forClass(clazz);
    verify(ctx).json(captor.capture());
    return captor.getValue();
  }
}
