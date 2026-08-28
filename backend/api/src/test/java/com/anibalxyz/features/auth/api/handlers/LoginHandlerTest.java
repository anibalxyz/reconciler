package com.anibalxyz.features.auth.api.handlers;

import static com.anibalxyz.shared.Constants.Auth.VALID_JWT_STRING;
import static com.anibalxyz.shared.Constants.Auth.VALID_REFRESH_RAW_TOKEN;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.anibalxyz.core.Result;
import com.anibalxyz.core.application.exception.FailureSignal;
import com.anibalxyz.features.auth.api.AuthCookieService;
import com.anibalxyz.features.auth.api.in.LoginRequest;
import com.anibalxyz.features.auth.api.out.AuthResponse;
import com.anibalxyz.features.auth.application.AuthenticateUser;
import com.anibalxyz.features.auth.application.out.AuthResult;
import com.anibalxyz.features.auth.domain.error.InvalidCredentialsError;
import com.anibalxyz.shared.ResultAsserts;
import com.anibalxyz.shared.UnitTest;
import io.javalin.http.Context;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

@DisplayName("Tests for LoginHandler")
public class LoginHandlerTest extends UnitTest {
  private static final ZonedDateTime FIXED_NOW =
      LocalDateTime.of(2025, 11, 25, 10, 0).atZone(ZoneId.of("America/Montevideo"));

  @Mock private Context ctx;
  @Mock private AuthenticateUser authenticateUser;
  @Mock private AuthCookieService authCookieService;

  @InjectMocks private LoginHandler loginHandler;

  @Test
  @DisplayName("login: given service result is failure, then throw FailureSignal with its error")
  public void login_serviceResultIsFailure_throwFailureSignal() {
    LoginRequest request = new LoginRequest("", "");

    Result<AuthResult, AuthenticateUser.Error> failedResult =
        Result.failure(
            new AuthenticateUser.Error.InvalidCredentials(new InvalidCredentialsError()));
    when(ctx.bodyAsClass(LoginRequest.class)).thenReturn(request);
    when(authenticateUser.execute(request.toCommand())).thenReturn(failedResult);

    var failure = ResultAsserts.failure(failedResult);
    assertThatThrownBy(() -> loginHandler.handle(ctx))
        .isInstanceOf(FailureSignal.class)
        .extracting(fs -> ((FailureSignal) fs).getError())
        .isEqualTo(failure);
  }

  @Test
  @DisplayName("login: given service returns success, then respond 200 with JWT and set cookie")
  public void login_serviceReturnsSuccess_respond200WithJWTAndSetCookie() {
    LoginRequest request = new LoginRequest("", "");
    AuthResult dummyAuthResult =
        new AuthResult(VALID_JWT_STRING, VALID_REFRESH_RAW_TOKEN, FIXED_NOW.toInstant());

    when(ctx.status(anyInt())).thenReturn(ctx);
    when(ctx.bodyAsClass(LoginRequest.class)).thenReturn(request);
    when(authenticateUser.execute(request.toCommand())).thenReturn(Result.success(dummyAuthResult));

    loginHandler.handle(ctx);

    verify(authCookieService)
        .setRefreshTokenCookie(
            ctx, dummyAuthResult.refreshToken().value(), dummyAuthResult.refreshTokenExpiryDate());

    verify(ctx).status(200);
    verify(ctx).json(new AuthResponse(dummyAuthResult.accessToken()));
  }
}
