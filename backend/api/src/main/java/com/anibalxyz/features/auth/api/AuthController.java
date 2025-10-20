package com.anibalxyz.features.auth.api;

import com.anibalxyz.features.auth.api.in.LoginRequest;
import com.anibalxyz.features.auth.application.AuthService;
import io.javalin.http.Context;

public class AuthController {
  private final AuthService authService;

  public AuthController(AuthService authService) {
    this.authService = authService;
  }

  public void login(Context ctx) {
    LoginRequest request =
        ctx.bodyValidator(LoginRequest.class)
            .check(r -> r.email() != null && !r.email().isBlank(), "Email is required")
            .check(r -> r.password() != null && !r.password().isBlank(), "Password is required")
            .get();
    String token = authService.authenticateUser(request);
    ctx.status(200).json(token);
  }
}
