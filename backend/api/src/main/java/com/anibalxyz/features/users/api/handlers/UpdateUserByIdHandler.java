package com.anibalxyz.features.users.api.handlers;

import static com.anibalxyz.features.common.api.Utils.getParamId;

import com.anibalxyz.core.application.exception.FailureSignal;
import com.anibalxyz.features.users.api.UserMapper;
import com.anibalxyz.features.users.api.in.UpdateUserRequest;
import com.anibalxyz.features.users.api.openapi.UpdateUserByIdEndpoint;
import com.anibalxyz.features.users.application.UpdateUserById;
import com.anibalxyz.features.users.domain.User;
import io.javalin.http.Context;
import io.javalin.http.Handler;
import org.jetbrains.annotations.NotNull;

public class UpdateUserByIdHandler implements Handler, UpdateUserByIdEndpoint {
  private final UpdateUserById updateUserById;

  public UpdateUserByIdHandler(UpdateUserById updateUserById) {
    this.updateUserById = updateUserById;
  }

  @Override
  public void handle(@NotNull Context ctx) {
    int id = getParamId(ctx);

    UpdateUserRequest updateUserRequest = ctx.bodyAsClass(UpdateUserRequest.class);
    User user =
        updateUserById.execute(id, updateUserRequest.toCommand()).orThrow(FailureSignal::new);

    ctx.status(200).json(UserMapper.toDetailResponse(user));
  }
}
