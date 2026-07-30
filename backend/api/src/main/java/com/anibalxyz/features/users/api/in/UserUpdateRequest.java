package com.anibalxyz.features.users.api.in;

import com.anibalxyz.features.users.application.in.UpdateUserCommand;
import com.anibalxyz.features.users.domain.Email;
import com.anibalxyz.features.users.domain.Name;
import com.anibalxyz.features.users.domain.PasswordHash;
import io.javalin.openapi.OpenApiExample;
import io.javalin.openapi.OpenApiIgnore;
import io.javalin.openapi.OpenApiStringValidation;

public record UserUpdateRequest(
    @OpenApiExample("John Doe")
        @OpenApiStringValidation(minLength = "1", maxLength = "" + Name.MAX_LENGTH)
        String name,
    @OpenApiExample("john.doe@example.com")
        @OpenApiStringValidation(
            format = "email",
            maxLength = "" + Email.MAX_LENGTH,
            pattern = Email.PATTERN)
        String email,
    @OpenApiExample("a-new-strong-password-456")
        @OpenApiStringValidation(
            minLength = "" + PasswordHash.MIN_LENGTH,
            maxLength = "" + PasswordHash.MAX_LENGTH)
        String password) {
  @OpenApiIgnore
  public UpdateUserCommand toCommand() {
    return new UpdateUserCommand(name, email, password);
  }
}
