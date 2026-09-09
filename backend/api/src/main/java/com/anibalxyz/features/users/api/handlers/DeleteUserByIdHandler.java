package com.anibalxyz.features.users.api.handlers;

import static com.anibalxyz.features.common.api.PathParams.getId;

import com.anibalxyz.core.api.FailureSignal;
import com.anibalxyz.features.users.api.openapi.DeleteUserByIdEndpoint;
import com.anibalxyz.features.users.application.DeleteUserById;
import io.javalin.http.Context;
import io.javalin.http.Handler;
import org.jetbrains.annotations.NotNull;

public class DeleteUserByIdHandler implements Handler, DeleteUserByIdEndpoint {
  private final DeleteUserById deleteUserById;

  public DeleteUserByIdHandler(DeleteUserById deleteUserById) {
    this.deleteUserById = deleteUserById;
  }

  @Override
  public void handle(@NotNull Context ctx) {
    int id = getId(ctx);
    deleteUserById.execute(id).orThrow(FailureSignal::new);
    ctx.status(204);
  }
}
