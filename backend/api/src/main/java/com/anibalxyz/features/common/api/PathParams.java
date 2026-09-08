package com.anibalxyz.features.common.api;

import io.javalin.http.Context;

public final class PathParams {

  private PathParams() {}

  /**
   * @throws InvalidIdFormat if the ID is missing or not a valid integer.
   */
  public static int getId(Context ctx) throws InvalidIdFormat {
    return ctx.pathParamAsClass("id", Integer.class)
        .getOrThrow((e) -> new InvalidIdFormat(ctx.pathParam("id")));
  }
}
