package com.anibalxyz.server.config.modules.runtime;

import com.anibalxyz.features.common.application.exception.FailureSignal;
import com.anibalxyz.server.api.ErrorMapper;
import com.anibalxyz.server.api.ErrorResult;
import com.anibalxyz.server.api.InfrastructureErrorMapper;
import io.javalin.Javalin;
import io.javalin.http.Context;
import io.javalin.http.HandlerType;
import io.javalin.http.HttpResponseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

// TODO: use a more semantic name for this class
public class ExceptionsConfig extends RuntimeConfig {

  private static final Logger log = LoggerFactory.getLogger(ExceptionsConfig.class);

  public ExceptionsConfig(Javalin server) {
    super(server);
  }

  /** {@inheritDoc} */
  @Override
  public void apply() {
    server.exception(
        FailureSignal.class,
        (e, ctx) -> {
          ErrorResult result = ErrorMapper.map(e.getError());
          String requestId = ctx.attribute("requestId");
          MDC.put("status", String.valueOf(result.status()));

          // TODO: add personalized logs within mapper

          ctx.status(result.status()).json(result.response().instance(requestId));
        });

    // Force Javalin's built-in exceptions to pass through our centralized mapper.
    // Will be obsolete when fully migrated to custom exceptions.
    server.exception(HttpResponseException.class, this::handleException);

    server.exception(
        Exception.class,
        (e, ctx) -> {
          // Avoid interfering with CORS preflight requests; let the CORS plugin
          // handle OPTIONS to prevent browser-side security blocks.
          if (ctx.method().equals(HandlerType.OPTIONS)) {
            return;
          }
          handleException(e, ctx);
        });
  }

  private void handleException(Exception e, Context ctx) {
    ErrorResult result = InfrastructureErrorMapper.map(e);
    String requestId = ctx.attribute("requestId");
    MDC.put("status", String.valueOf(result.status()));

    if (result.status() >= 500) {
      log.error("Internal Server Error: {}", e.getMessage(), e);
    } else {
      log.debug("Client error: {}", e.getMessage());
    }

    ctx.status(result.status()).json(result.response().instance(requestId));
  }
}
