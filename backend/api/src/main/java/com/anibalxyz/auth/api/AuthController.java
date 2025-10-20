package com.anibalxyz.auth.api;

import com.anibalxyz.auth.api.in.LoginRequest;
import com.anibalxyz.auth.application.AuthService;
import io.javalin.http.Context;

public class AuthController {
  private final AuthService authService;

  public AuthController(AuthService authService) {
    this.authService = authService;
  }

  public void login(Context ctx) {
    LoginRequest request = ctx.bodyAsClass(LoginRequest.class);
    String token = authService.authenticateUser(request);
    ctx.status(200).json(token);
  }
}
