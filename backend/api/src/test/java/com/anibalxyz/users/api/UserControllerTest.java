package com.anibalxyz.users.api;

import com.anibalxyz.users.api.out.UserDetailResponse;
import com.anibalxyz.users.application.UserService;
import com.anibalxyz.users.domain.Email;
import com.anibalxyz.users.domain.PasswordHash;
import com.anibalxyz.users.domain.User;
import io.javalin.http.Context;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class UserControllerTest {

  @Mock private UserService userService;

  @Mock private Context ctx;

  @InjectMocks private UserController userController;

  @Test
  public void getAllUsers() {
    List<User> fakeUsers =
        List.of(
            new User(
                1,
                "John Doe",
                new Email("john.doe@example.com"),
                PasswordHash.generate("12345678"),
                LocalDateTime.now(),
                LocalDateTime.now()),
            new User(
                2,
                "Jane Smith",
                new Email("jane.smith@example.com"),
                PasswordHash.generate("87654321"),
                LocalDateTime.now(),
                LocalDateTime.now()));

    when(this.userService.getAllUsers()).thenReturn(fakeUsers);

    this.userController.getAllUsers(this.ctx);

    ArgumentCaptor<List<UserDetailResponse>> captor = ArgumentCaptor.forClass(List.class);
    verify(this.ctx).json(captor.capture());
    List<UserDetailResponse> response = captor.getValue();

    assertThat(response).hasSize(2);
    assertThat(response.get(0).id()).isEqualTo(fakeUsers.get(0).getId());
    assertThat(response.get(0).name()).isEqualTo(fakeUsers.get(0).getName());
    assertThat(response.get(0).email()).isEqualTo(fakeUsers.get(0).getEmail().value());
    assertThat(response.get(1).id()).isEqualTo(fakeUsers.get(1).getId());
  }
}
