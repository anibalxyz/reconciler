package com.anibalxyz.features.users.api.handlers;

import com.anibalxyz.core.application.exception.FailureSignal;
import com.anibalxyz.features.users.api.UserMapper;
import com.anibalxyz.features.users.api.in.CreateUserRequest;
import com.anibalxyz.features.users.api.openapi.CreateUserEndpoint;
import com.anibalxyz.features.users.application.CreateUser;
import com.anibalxyz.features.users.domain.User;
import io.javalin.http.Context;
import io.javalin.http.Handler;
import org.jetbrains.annotations.NotNull;

public class CreateUserHandler implements Handler, CreateUserEndpoint {
  private final CreateUser createUser;

  public CreateUserHandler(CreateUser createUser) {
    this.createUser = createUser;
  }

  @Override
  public void handle(@NotNull Context ctx) {
    CreateUserRequest request = ctx.bodyAsClass(CreateUserRequest.class);

    User user = createUser.execute(request.toCommand()).orThrow(FailureSignal::new);

    ctx.status(201).json(UserMapper.toCreateResponse(user));
  }
}
