package com.anibalxyz.features.common.api;

import io.javalin.http.BadRequestResponse;
import io.javalin.http.Context;

public final class Utils {

  /**
   * @throws BadRequestResponse if the ID is missing or not a valid integer.
   */
  public static int getParamId(Context ctx) throws BadRequestResponse {
    // TODO: migrate to a personalized error (at the moment this is an edge case so it does not
    //       matter)
    return ctx.pathParamAsClass("id", Integer.class)
        .getOrThrow(e -> new BadRequestResponse("Invalid ID format. Must be a number."));
  }
}
