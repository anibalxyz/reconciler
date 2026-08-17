package com.anibalxyz.features.users.api.handlers;

import static com.anibalxyz.shared.Constants.Users.*;
import static com.anibalxyz.shared.Helpers.stubStatusChaining;
import static com.anibalxyz.shared.Helpers.whenGettingPathParamId;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.verify;

import com.anibalxyz.core.Result;
import com.anibalxyz.core.application.exception.FailureSignal;
import com.anibalxyz.features.users.api.UserMapper;
import com.anibalxyz.features.users.api.in.UpdateUserRequest;
import com.anibalxyz.features.users.application.UpdateUserById;
import com.anibalxyz.features.users.application.in.UpdateUserCommand;
import com.anibalxyz.features.users.domain.User;
import com.anibalxyz.shared.UnitTest;
import io.javalin.http.BadRequestResponse;
import io.javalin.http.Context;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("Tests for UpdateUserByIdHandler")
public class UpdateUserByIdHandlerTest extends UnitTest {
  @Mock private UpdateUserById updateUserById;
  @Mock private Context ctx;
  @InjectMocks private UpdateUserByIdHandler updateUserByIdHandler;

  @Test
  @DisplayName("updateUserById: given an invalid id, then throw BadRequestResponse")
  public void updateUserById_invalidId_throwBadRequestResponse() {
    whenGettingPathParamId(ctx).thenThrow(new BadRequestResponse());

    assertThatThrownBy(() -> updateUserByIdHandler.handle(ctx))
        .isInstanceOf(BadRequestResponse.class);
  }

  @Test
  @DisplayName("updateUserById: given the service returns Error, then throw FailureSignal")
  public void updateUserById_serviceReturnsUpdateUserByIdError_throwFailureSignal() {
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
  @DisplayName("updateUserById: given the service returns User, then respond 200 with updated user")
  public void updateUserById_serviceReturnsUser_respond200WithUpdatedUser() {
    UpdateUserRequest request =
        new UpdateUserRequest(VALID_EMAIL_STRING, VALID_EMAIL_STRING, VALID_PASSWORD_STRING);

    User fakeUser = VALID_USER;

    stubStatusChaining(ctx);
    whenGettingPathParamId(ctx).thenReturn(fakeUser.id());
    stubStatusChaining(ctx);
    when(ctx.bodyAsClass(UpdateUserRequest.class)).thenReturn(request);
    when(updateUserById.execute(fakeUser.id(), request.toCommand()))
        .thenReturn(Result.success(fakeUser));

    updateUserByIdHandler.handle(ctx);

    verify(ctx).status(200);
    verify(ctx).json(UserMapper.toDetailResponse(fakeUser));
  }
}
