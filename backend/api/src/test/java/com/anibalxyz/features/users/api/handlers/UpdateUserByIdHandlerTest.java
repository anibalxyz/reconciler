package com.anibalxyz.features.users.api.handlers;

import static com.anibalxyz.shared.Constants.Users.*;
import static com.anibalxyz.shared.Helpers.stubStatusChaining;
import static com.anibalxyz.shared.Helpers.whenGettingPathParamId;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.verify;

import com.anibalxyz.core.api.exception.InvalidIdFormat;
import com.anibalxyz.core.Result;
import com.anibalxyz.core.application.exception.FailureSignal;
import com.anibalxyz.features.users.api.UserMapper;
import com.anibalxyz.features.users.api.in.UpdateUserRequest;
import com.anibalxyz.features.users.application.UpdateUserById;
import com.anibalxyz.features.users.application.in.UpdateUserCommand;
import com.anibalxyz.features.users.domain.User;
import com.anibalxyz.shared.UnitTest;
import io.javalin.http.Context;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

@DisplayName("Tests for UpdateUserByIdHandler")
public class UpdateUserByIdHandlerTest extends UnitTest {
  @Mock private UpdateUserById updateUserById;
  @Mock private Context ctx;
  @InjectMocks private UpdateUserByIdHandler updateUserByIdHandler;

  @Test
  @DisplayName("handle: given an invalid id, then throw InvalidIdFormat")
  public void handle_invalidId_throwInvalidIdFormat() {
    whenGettingPathParamId(ctx).thenThrow(new InvalidIdFormat("abc"));

    assertThatThrownBy(() -> updateUserByIdHandler.handle(ctx))
        .isInstanceOf(InvalidIdFormat.class);
  }

  @Test
  @DisplayName("handle: given the service returns Error, then throw FailureSignal")
  public void handle_serviceReturnsUpdateUserByIdError_throwFailureSignal() {
    UpdateUserRequest request = mock(UpdateUserRequest.class);
    UpdateUserCommand command = mock(UpdateUserCommand.class);
    when(request.toCommand()).thenReturn(command);
    whenGettingPathParamId(ctx).thenReturn(1);

    when(ctx.bodyAsClass(UpdateUserRequest.class)).thenReturn(request);
    when(updateUserById.execute(1, command))
        .thenReturn(Result.failure(new UpdateUserById.Error.EmptyCommand()));

    assertThatThrownBy(() -> updateUserByIdHandler.handle(ctx))
        .isInstanceOf(FailureSignal.class)
        .extracting(fs -> ((FailureSignal) fs).getError())
        .isInstanceOf(UpdateUserById.Error.class);
  }

  @Test
  @DisplayName("handle: given the service returns User, then respond 200 with updated user")
  public void handle_serviceReturnsUser_respond200WithUpdatedUser() {
    UpdateUserRequest request =
        new UpdateUserRequest(VALID_EMAIL_STRING, VALID_EMAIL_STRING, VALID_PASSWORD_STRING);

    User fakeUser = VALID_USER;

    stubStatusChaining(ctx);
    whenGettingPathParamId(ctx).thenReturn(fakeUser.id().value());
    stubStatusChaining(ctx);
    when(ctx.bodyAsClass(UpdateUserRequest.class)).thenReturn(request);
    when(updateUserById.execute(fakeUser.id().value(), request.toCommand()))
        .thenReturn(Result.success(fakeUser));

    updateUserByIdHandler.handle(ctx);

    verify(ctx).status(200);
    verify(ctx).json(UserMapper.toDetailResponse(fakeUser));
  }
}
