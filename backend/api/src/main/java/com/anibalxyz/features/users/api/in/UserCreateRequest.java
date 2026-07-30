package com.anibalxyz.features.users.api.in;

import com.anibalxyz.features.users.application.in.CreateUserCommand;
import com.anibalxyz.features.users.domain.Email;
import com.anibalxyz.features.users.domain.Name;
import com.anibalxyz.features.users.domain.PasswordHash;
import io.javalin.openapi.OpenApiExample;
import io.javalin.openapi.OpenApiIgnore;
import io.javalin.openapi.OpenApiRequired;
import io.javalin.openapi.OpenApiStringValidation;

public record UserCreateRequest(
    @OpenApiExample("John Doe")
        @OpenApiRequired
        @OpenApiStringValidation(minLength = "1", maxLength = "" + Name.MAX_LENGTH)
        String name,
    @OpenApiExample("john.doe@example.com")
        @OpenApiRequired
        @OpenApiStringValidation(
            format = "email",
            maxLength = "" + Email.MAX_LENGTH,
            pattern = Email.PATTERN)
        String email,
    @OpenApiExample("strong-password-123")
        @OpenApiRequired
        @OpenApiStringValidation(
            minLength = "" + PasswordHash.MIN_LENGTH,
            maxLength = "" + PasswordHash.MAX_LENGTH)
        String password) {

  // NOTE: utility method. May be from a common interface (e.g. Request)
  @OpenApiIgnore
  public CreateUserCommand toCommand() {
    return new CreateUserCommand(name, email, password);
  }
}
