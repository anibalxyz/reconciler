package com.anibalxyz.features.users.api.handlers;

import static com.anibalxyz.features.common.api.Utils.getParamId;

import com.anibalxyz.core.application.exception.FailureSignal;
import com.anibalxyz.features.users.api.UserMapper;
import com.anibalxyz.features.users.api.openapi.GetUserByIdEndpoint;
import com.anibalxyz.features.users.application.GetUserById;
import com.anibalxyz.features.users.domain.User;
import io.javalin.http.Context;
import io.javalin.http.Handler;
import org.jetbrains.annotations.NotNull;

public class GetUserByIdHandler implements Handler, GetUserByIdEndpoint {
  private final GetUserById getUserById;

  public GetUserByIdHandler(GetUserById getUserById) {
    this.getUserById = getUserById;
  }

  @Override
  public void handle(@NotNull Context ctx) {
    int id = getParamId(ctx);
    User user = getUserById.execute(id).orThrow(FailureSignal::new);

    ctx.status(200).json(UserMapper.toDetailResponse(user));
  }
}
