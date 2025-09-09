package com.anibalxyz.users.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

import com.anibalxyz.users.api.out.UserDetailResponse;
import com.anibalxyz.users.application.UserService;
import com.anibalxyz.users.domain.Email;
import com.anibalxyz.users.domain.PasswordHash;
import com.anibalxyz.users.domain.User;
import io.javalin.http.BadRequestResponse;
import io.javalin.http.Context;
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
    when(this.userService.getAllUsers()).thenReturn(fakeUsers);
    this.userController.getAllUsers(this.ctx);

    ArgumentCaptor<List<UserDetailResponse>> captor = ArgumentCaptor.forClass(List.class);
    verify(this.ctx).json(captor.capture());
    List<UserDetailResponse> actual = captor.getValue();
    List<UserDetailResponse> expected =
        fakeUsers.stream().map(UserMapper::toDetailResponse).toList();

    // then return users as JSON
    assertThat(actual).isEqualTo(expected);
  }

  @Test
  @DisplayName("Given there are no users, when getAllUsers is called, then return empty JSON array")
  public void getAllUsers_returnEmptyJsonArray() {
    List<User> fakeUsers = List.of();

    when(this.userService.getAllUsers()).thenReturn(fakeUsers);
    this.userController.getAllUsers(this.ctx);

    ArgumentCaptor<List<UserDetailResponse>> captor = ArgumentCaptor.forClass(List.class);
    verify(this.ctx).json(captor.capture());
    List<UserDetailResponse> actual = captor.getValue();
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

    when(this.userService.getUserById(id)).thenReturn(optionalUser);

    Validator<Integer> mockValidator = mock(Validator.class);
    when(mockValidator.getOrThrow(any())).thenReturn(id);
    when(this.ctx.pathParamAsClass("id", Integer.class)).thenReturn(mockValidator);

    this.userController.getUserById(this.ctx);

    ArgumentCaptor<UserDetailResponse> captor = ArgumentCaptor.forClass(UserDetailResponse.class);
    verify(this.ctx).json(captor.capture());
    UserDetailResponse actual = captor.getValue();
    UserDetailResponse expected = UserMapper.toDetailResponse(fakeUser);

    assertThat(actual).isEqualTo(expected);
  }

  @Test
  @DisplayName("Given a user ID that does not exist, when getUserById is called, then return 404")
  public void getUserById_userDoesNotExist_return404() {
    int id = 1;

    Validator<Integer> mockValidator = mock(Validator.class);
    when(mockValidator.getOrThrow(any())).thenReturn(id);
    when(this.ctx.pathParamAsClass("id", Integer.class)).thenReturn(mockValidator);

    when(this.userService.getUserById(id)).thenReturn(Optional.empty());
    when(this.ctx.status(anyInt())).thenReturn(this.ctx);

    this.userController.getUserById(this.ctx);

    verify(this.ctx).status(404);
    ArgumentCaptor<Map<String, String>> captor = ArgumentCaptor.forClass(Map.class);
    verify(this.ctx).json(captor.capture());

    Map<String, String> actual = captor.getValue();
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
    when(this.ctx.pathParamAsClass("id", Integer.class)).thenReturn(mockValidator);

    // What really tests is that the exception is thrown and not caught
    assertThatThrownBy(() -> this.userController.getUserById(this.ctx))
        .isInstanceOf(BadRequestResponse.class);
  }
}
