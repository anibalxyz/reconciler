package com.anibalxyz.server;

import io.javalin.http.Context;

public class ContextProvider {
  private static final ThreadLocal<Context> contextThreadLocal = new ThreadLocal<>();

  public static void set(Context context) {
    contextThreadLocal.set(context);
  }

  public static void clear() {
    contextThreadLocal.remove();
  }

  public static Context get() {
    return contextThreadLocal.get();
  }
}
