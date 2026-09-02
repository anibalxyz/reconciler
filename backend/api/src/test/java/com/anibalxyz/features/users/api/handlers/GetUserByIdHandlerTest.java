package com.anibalxyz.features.users.api.handlers;

import static com.anibalxyz.shared.Constants.Users.VALID_USER;
import static com.anibalxyz.shared.Helpers.stubStatusChaining;
import static com.anibalxyz.shared.Helpers.whenGettingPathParamId;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.anibalxyz.core.Result;
import com.anibalxyz.core.application.exception.FailureSignal;
import com.anibalxyz.features.users.api.UserMapper;
import com.anibalxyz.features.users.application.GetUserById;
import com.anibalxyz.features.users.domain.User;
import com.anibalxyz.features.users.domain.error.UserNotFoundError;
import com.anibalxyz.shared.UnitTest;
import io.javalin.http.BadRequestResponse;
import io.javalin.http.Context;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

@DisplayName("Tests for GetUserByIdHandler")
public class GetUserByIdHandlerTest extends UnitTest {
  @Mock private GetUserById getUserById;
  @Mock private Context ctx;
  @InjectMocks private GetUserByIdHandler getUserByIdHandler;

  @Test
  @DisplayName("handle: given the service returns UserNotFoundError, then throw FailureSignal")
  public void handle_serviceReturnsUserNotFoundError_throwFailureSignal() {
    int nonExistingId = 999;
    whenGettingPathParamId(ctx).thenReturn(nonExistingId);
    when(getUserById.execute(nonExistingId))
        .thenReturn(Result.failure(UserNotFoundError.byId(nonExistingId)));

    assertThatThrownBy(() -> getUserByIdHandler.handle(ctx))
        .isInstanceOf(FailureSignal.class)
        .extracting(e -> ((FailureSignal) e).getError())
        .isInstanceOf(UserNotFoundError.class);
  }

  @Test
  @DisplayName("handle: given an invalid id, then throw BadRequestResponse")
  public void handle_invalidId_throwBadRequestResponse() {
    whenGettingPathParamId(ctx).thenThrow(new BadRequestResponse());

    assertThatThrownBy(() -> getUserByIdHandler.handle(ctx)).isInstanceOf(BadRequestResponse.class);
  }

  @Test
  @DisplayName("handle: given the service returns User, then return 200 with User")
  public void handle_serviceReturnsUser_respond200WithUser() {
    User fakeUser = VALID_USER;

    stubStatusChaining(ctx);
    whenGettingPathParamId(ctx).thenReturn(fakeUser.id().value());
    when(getUserById.execute(fakeUser.id().value())).thenReturn(Result.success(fakeUser));

    getUserByIdHandler.handle(ctx);

    verify(ctx).status(200);
    verify(ctx).json(UserMapper.toDetailResponse(fakeUser));
  }
}
