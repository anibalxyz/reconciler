package com.anibalxyz.features.auth.api.handlers;

import static com.anibalxyz.shared.Constants.Auth.*;
import static com.anibalxyz.shared.Helpers.*;
import static org.mockito.Mockito.*;

import com.anibalxyz.features.auth.api.AuthCookieService;
import com.anibalxyz.features.auth.application.Logout;
import com.anibalxyz.shared.UnitTest;
import io.javalin.http.Context;
import java.time.*;
import org.junit.jupiter.api.*;
import org.mockito.InjectMocks;
import org.mockito.Mock;

@DisplayName("Tests for LogoutHandler")
public class LogoutHandlerTest extends UnitTest {
  @Mock private Context ctx;
  @Mock private AuthCookieService authCookieService;
  @Mock private Logout logout;

  @InjectMocks private LogoutHandler logoutHandler;

  @Test
  @DisplayName("handle: given existing refresh token, then clear cookie and revoke token")
  void handle_existingRefreshToken_clearCookieAndRevokeToken() {
    when(ctx.status(anyInt())).thenReturn(ctx);
    when(authCookieService.getRefreshTokenCookie(ctx)).thenReturn(VALID_REFRESH_RAW_TOKEN_STRING);

    logoutHandler.handle(ctx);

    verify(logout).execute(VALID_REFRESH_RAW_TOKEN_STRING);
    verify(ctx).status(204);

    verify(authCookieService).clearRefreshTokenCookie(ctx);
  }
}
