package com.anibalxyz.features.auth.api.handlers;

import static com.anibalxyz.shared.Constants.Auth.*;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

import com.anibalxyz.core.Result;
import com.anibalxyz.core.application.exception.FailureSignal;
import com.anibalxyz.features.auth.api.AuthCookieService;
import com.anibalxyz.features.auth.api.exception.MissingRefreshTokenCookie;
import com.anibalxyz.features.auth.api.out.AuthResponse;
import com.anibalxyz.features.auth.application.RefreshTokens;
import com.anibalxyz.features.auth.application.out.AuthResult;
import com.anibalxyz.features.auth.domain.error.InvalidRefreshTokenError;
import com.anibalxyz.shared.ResultAsserts;
import com.anibalxyz.shared.UnitTest;
import io.javalin.http.Context;
import java.time.*;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.mockito.InjectMocks;
import org.mockito.Mock;

@DisplayName("Tests for RefreshTokensHandler")
public class RefreshTokensHandlerTest extends UnitTest {
  private static final ZonedDateTime FIXED_NOW =
      LocalDateTime.of(2025, 11, 25, 10, 0).atZone(ZoneId.of("America/Montevideo"));

  @Mock private Context ctx;
  @Mock private AuthCookieService authCookieService;
  @Mock private RefreshTokens refreshTokens;

  @InjectMocks private RefreshTokensHandler refreshTokensHandler;

  @ParameterizedTest
  @NullAndEmptySource
  @DisplayName("handle: given missing refresh token cookie, then throw MissingRefreshTokenCookie")
  public void handle_missingRefreshTokenCookie_throwMissingRefreshTokenCookie(String value) {
    when(authCookieService.getRefreshTokenCookie(ctx)).thenReturn(value);

    assertThatThrownBy(() -> refreshTokensHandler.handle(ctx))
        .isInstanceOf(MissingRefreshTokenCookie.class);
  }

  @Test
  @DisplayName("handle: given service result is failure, then throw FailureSignal with its error")
  public void handle_serviceReturnsRefreshTokensError_throwFailureSignal() {
    Result<AuthResult, RefreshTokens.Error> failedResult =
        Result.failure(new RefreshTokens.Error.InvalidToken(InvalidRefreshTokenError.notFound()));

    when(authCookieService.getRefreshTokenCookie(ctx)).thenReturn(VALID_REFRESH_RAW_TOKEN_STRING);
    when(refreshTokens.execute(VALID_REFRESH_RAW_TOKEN_STRING)).thenReturn(failedResult);

    var failure = ResultAsserts.failure(failedResult);
    assertThatThrownBy(() -> refreshTokensHandler.handle(ctx))
        .isInstanceOf(FailureSignal.class)
        .extracting(fs -> ((FailureSignal) fs).getError())
        .isEqualTo(failure);
  }

  @Test
  @DisplayName(
      "handle: given existing refresh token and service returns success, then respond 200 with refreshed tokens")
  public void handle_existingRefreshTokenAndServiceReturnsSuccess_respond200WithRefreshedTokens() {
    AuthResult result =
        new AuthResult(
            VALID_JWT_STRING,
            VALID_REFRESH_RAW_TOKEN,
            FIXED_NOW.toInstant().plus(2, ChronoUnit.DAYS));
    AuthResponse expectedResponse = new AuthResponse(result.accessToken());

    when(ctx.status(anyInt())).thenReturn(ctx);
    when(authCookieService.getRefreshTokenCookie(ctx)).thenReturn(VALID_REFRESH_RAW_TOKEN_STRING);
    when(refreshTokens.execute(VALID_REFRESH_RAW_TOKEN_STRING)).thenReturn(Result.success(result));

    refreshTokensHandler.handle(ctx);

    verify(authCookieService)
        .setRefreshTokenCookie(ctx, result.refreshToken().value(), result.refreshTokenExpiryDate());

    verify(ctx).status(200);
    verify(ctx).json(expectedResponse);
  }
}
