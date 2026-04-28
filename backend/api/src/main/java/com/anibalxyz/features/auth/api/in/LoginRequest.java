package com.anibalxyz.features.auth.api.in;

import com.anibalxyz.features.auth.application.in.LoginCommand;
import io.javalin.openapi.OpenApiExample;
import io.javalin.openapi.OpenApiIgnore;
import io.javalin.openapi.OpenApiRequired;
import io.javalin.openapi.OpenApiStringValidation;

public record LoginRequest(
    @OpenApiExample("john.doe@example.com")
        @OpenApiRequired
        @OpenApiStringValidation(format = "email")
        String email,
    @OpenApiExample("strong-password-123") @OpenApiRequired String password) {

  @OpenApiIgnore
  public LoginCommand toCommand() {
    return new LoginCommand(email, password);
  }
}
