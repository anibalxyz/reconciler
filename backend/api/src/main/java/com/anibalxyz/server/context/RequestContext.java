package com.anibalxyz.server.context;

import io.javalin.http.Context;
import java.util.UUID;
import org.slf4j.MDC;

/**
 * Manages per-request contextual data via SLF4J MDC.
 *
 * <p>MDC (Mapped Diagnostic Context) is a thread-local key-value store provided by SLF4J. Any value
 * placed here is automatically included in every log statement made by any class on that thread,
 * without needing to pass it as a parameter.
 *
 * <p>All values are cleared at the end of each request to prevent leaking into future requests
 * (thread reuse via virtual threads or thread pools).
 */
public class RequestContext {

  public static final String REQUEST_ID_KEY = "request_id";

  private RequestContext() {}

  /**
   * Generates a request ID and seeds the MDC for this request.
   *
   * <p>The request ID follows the format {@code req-<UUID>}.
   *
   * @param ctx the Javalin request context
   * @return the request ID that was assigned
   */
  public static String initialize(Context ctx) {
    String requestId = "req-" + UUID.randomUUID();

    MDC.put(REQUEST_ID_KEY, requestId);
    MDC.put("method", ctx.method().name());
    MDC.put("path", ctx.path());

    return requestId;
  }

  /**
   * Retrieves the current request ID from the MDC.
   *
   * @return the request ID, or null if called outside a request context
   */
  public static String getCurrentRequestId() {
    return MDC.get(REQUEST_ID_KEY);
  }

  /**
   * Clears all MDC values for this thread.
   *
   * <p>MUST be called at the end of every request to prevent stale data leaking into the next
   * request (thread reuse).
   */
  public static void clear() {
    MDC.clear();
  }
}
