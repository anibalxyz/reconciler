package com.anibalxyz.features.users.api.handlers;

import static com.anibalxyz.shared.Helpers.whenGettingPathParamId;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.anibalxyz.core.Result;
import com.anibalxyz.core.application.exception.FailureSignal;
import com.anibalxyz.features.users.application.DeleteUserById;
import com.anibalxyz.features.users.domain.error.UserNotFoundError;
import com.anibalxyz.shared.UnitTest;
import io.javalin.http.BadRequestResponse;
import io.javalin.http.Context;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

@DisplayName("Tests for DeleteUserByIdHandler")
public class DeleteUserByIdHandlerTest extends UnitTest {
  @Mock private DeleteUserById deleteUserById;
  @Mock private Context ctx;
  @InjectMocks private DeleteUserByIdHandler deleteUserByIdHandler;

  @Test
  @DisplayName("deleteUserById: given an invalid id, then throw BadRequestResponse")
  public void deleteUserById_invalidId_throwBadRequestResponse() {
    whenGettingPathParamId(ctx).thenThrow(new BadRequestResponse());

    assertThatThrownBy(() -> deleteUserByIdHandler.handle(ctx))
        .isInstanceOf(BadRequestResponse.class);
  }

  @Test
  @DisplayName(
      "deleteUserById: given the service returns UserNotFoundError, then throw FailureSignal")
  public void deleteUserById_serviceReturnsUserNotFoundError_throwFailureSignal() {
    int nonExistingId = 999;
    whenGettingPathParamId(ctx).thenReturn(nonExistingId);
    when(deleteUserById.execute(nonExistingId))
        .thenReturn(Result.failure(UserNotFoundError.byId(nonExistingId)));

    assertThatThrownBy(() -> deleteUserByIdHandler.handle(ctx))
        .isInstanceOf(FailureSignal.class)
        .extracting(fs -> ((FailureSignal) fs).getError())
        .isInstanceOf(UserNotFoundError.class);
  }

  @Test
  @DisplayName("deleteUserById: given the service returns success, then respond 204 no content")
  public void deleteUserById_serviceReturnsSuccess_respond204NoContent() {
    int validId = 1;
    whenGettingPathParamId(ctx).thenReturn(validId);
    when(deleteUserById.execute(validId)).thenReturn(Result.success(null));

    deleteUserByIdHandler.handle(ctx);

    verify(ctx).status(204);
    verify(ctx, never()).json(any());
  }
}
