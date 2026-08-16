package com.anibalxyz.features.users.api.in;

import com.anibalxyz.features.users.application.in.CreateUserCommand;
import com.anibalxyz.features.users.domain.Email;
import com.anibalxyz.features.users.domain.Name;
import com.anibalxyz.features.users.domain.Password;
import io.javalin.openapi.OpenApiExample;
import io.javalin.openapi.OpenApiIgnore;
import io.javalin.openapi.OpenApiRequired;
import io.javalin.openapi.OpenApiStringValidation;

public record CreateUserRequest(
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
            minLength = "" + Password.MIN_LENGTH,
            maxLength = "" + Password.MAX_LENGTH)
        String password) {

  @OpenApiIgnore
  public CreateUserCommand toCommand() {
    return new CreateUserCommand(name, email, password);
  }
}
