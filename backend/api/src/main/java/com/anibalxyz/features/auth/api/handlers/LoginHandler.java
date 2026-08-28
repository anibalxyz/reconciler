package com.anibalxyz.features.auth.api.handlers;

import com.anibalxyz.core.application.exception.FailureSignal;
import com.anibalxyz.features.auth.api.AuthCookieService;
import com.anibalxyz.features.auth.api.in.LoginRequest;
import com.anibalxyz.features.auth.api.openapi.LoginEndpoint;
import com.anibalxyz.features.auth.api.out.AuthResponse;
import com.anibalxyz.features.auth.application.AuthenticateUser;
import com.anibalxyz.features.auth.application.in.LoginCommand;
import com.anibalxyz.features.auth.application.out.AuthResult;
import io.javalin.http.Context;
import io.javalin.http.Handler;
import org.jetbrains.annotations.NotNull;

public class LoginHandler implements Handler, LoginEndpoint {
  private final AuthCookieService authCookieService;
  private final AuthenticateUser authenticateUser;

  public LoginHandler(AuthCookieService authCookieService, AuthenticateUser authenticateUser) {
    this.authCookieService = authCookieService;
    this.authenticateUser = authenticateUser;
  }

  @Override
  public void handle(@NotNull Context ctx) {

    LoginCommand command = ctx.bodyAsClass(LoginRequest.class).toCommand();
    AuthResult authResult = authenticateUser.execute(command).orThrow(FailureSignal::new);

    authCookieService.setRefreshTokenCookie(
        ctx, authResult.refreshToken().value(), authResult.refreshTokenExpiryDate());
    ctx.status(200).json(new AuthResponse(authResult.accessToken()));
  }
}
