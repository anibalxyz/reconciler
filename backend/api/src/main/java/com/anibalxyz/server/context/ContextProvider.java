package com.anibalxyz.server.context;

import io.javalin.http.Context;

public class ContextProvider {
  private static final ThreadLocal<Context> contextThreadLocal = new ThreadLocal<>();

  public static void set(Context ctx) {
    contextThreadLocal.set(ctx);
  }

  public static void clear() {
    contextThreadLocal.remove();
  }

  public static Context get() {
    return contextThreadLocal.get();
  }
}
