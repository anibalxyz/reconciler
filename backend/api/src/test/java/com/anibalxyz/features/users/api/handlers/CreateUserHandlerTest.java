package com.anibalxyz.features.users.api.handlers;

import static com.anibalxyz.shared.Constants.Users.VALID_PASSWORD_STRING;
import static com.anibalxyz.shared.Constants.Users.VALID_USER;
import static com.anibalxyz.shared.Helpers.stubStatusChaining;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.verify;

import com.anibalxyz.core.Result;
import com.anibalxyz.core.application.ValidationNotification;
import com.anibalxyz.core.application.exception.FailureSignal;
import com.anibalxyz.features.users.api.UserMapper;
import com.anibalxyz.features.users.api.in.CreateUserRequest;
import com.anibalxyz.features.users.application.CreateUser;
import com.anibalxyz.features.users.application.in.CreateUserCommand;
import com.anibalxyz.features.users.domain.User;
import com.anibalxyz.shared.UnitTest;
import io.javalin.http.Context;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

@DisplayName("Tests for CreateUserHandler")
public class CreateUserHandlerTest extends UnitTest {
  @Mock private CreateUser createUser;
  @Mock private Context ctx;
  @InjectMocks private CreateUserHandler createUserHandler;

  @Test
  @DisplayName("handle: given the service returns ValidationNotification, then throw FailureSignal")
  public void handle_serviceReturnsValidationNotification_throwFailureSignal() {
    CreateUserRequest request = mock(CreateUserRequest.class);
    CreateUserCommand command = mock(CreateUserCommand.class);
    when(request.toCommand()).thenReturn(command);

    when(ctx.bodyAsClass(CreateUserRequest.class)).thenReturn(request);
    when(createUser.execute(command)).thenReturn(Result.failure(new ValidationNotification<>()));

    assertThatThrownBy(() -> createUserHandler.handle(ctx))
        .isInstanceOf(FailureSignal.class)
        .extracting(fs -> ((FailureSignal) fs).getError())
        .isInstanceOf(ValidationNotification.class);
  }

  @Test
  @DisplayName("handle: given the service returns User, then respond 201 with new user")
  public void handle_serviceReturnsUser_respond201WithNewUser() {
    User user = VALID_USER;
    CreateUserRequest request =
        new CreateUserRequest(user.name().value(), user.email().value(), VALID_PASSWORD_STRING);

    stubStatusChaining(ctx);
    when(ctx.bodyAsClass(CreateUserRequest.class)).thenReturn(request);
    when(createUser.execute(request.toCommand())).thenReturn(Result.success(user));

    createUserHandler.handle(ctx);

    verify(ctx).status(201);
    verify(ctx).json(UserMapper.toCreateResponse(user));
  }
}
