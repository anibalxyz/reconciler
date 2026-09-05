package com.anibalxyz.features.common.api;

import com.anibalxyz.core.api.exception.InvalidIdFormat;
import io.javalin.http.Context;

public final class Utils {

  private Utils() {}

  /**
   * @throws InvalidIdFormat if the ID is missing or not a valid integer.
   */
  public static int getParamId(Context ctx) throws InvalidIdFormat {
    return ctx.pathParamAsClass("id", Integer.class)
        .getOrThrow((e) -> new InvalidIdFormat(ctx.pathParam("id")));
  }
}
