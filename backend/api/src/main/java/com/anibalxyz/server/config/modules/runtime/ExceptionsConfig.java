package com.anibalxyz.server.config.modules.runtime;

import static net.logstash.logback.argument.StructuredArguments.kv;

import com.anibalxyz.features.common.application.exception.FailureSignal;
import com.anibalxyz.server.api.ErrorMapper;
import com.anibalxyz.server.api.ErrorResult;
import com.anibalxyz.server.api.InfrastructureErrorMapper;
import com.anibalxyz.server.api.LogEntry;
import com.anibalxyz.server.context.RequestContext;
import io.javalin.Javalin;
import io.javalin.http.Context;
import io.javalin.http.HandlerType;
import io.javalin.http.HttpResponseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.slf4j.event.Level;

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
          String requestId = ctx.attribute(RequestContext.REQUEST_ID_KEY);
          MDC.put("status", String.valueOf(result.status()));
          emitLogEntry(result);

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
    String requestId = ctx.attribute(RequestContext.REQUEST_ID_KEY);
    MDC.put("status", String.valueOf(result.status()));
    emitLogEntry(result);

    if (result.status() >= 500) {
      log.error(
          "{}: {}",
          e.getClass().getSimpleName(),
          e.getMessage(),
          kv("error_code", result.response().code()));
    } else {
      log.debug(
          "{}: {}",
          e.getClass().getSimpleName(),
          e.getMessage(),
          kv("error_code", result.response().code()));
    }

    ctx.status(result.status()).json(result.response().instance(requestId));
  }

  private void emitLogEntry(ErrorResult result) {
    LogEntry entry = result.logEntry();
    if (entry == null) return;
    String errorCode = result.response().code();
    Level level = entry.level();
    String message = entry.message();
    Object[] args = entry.args();

    if (errorCode != null && !errorCode.isEmpty()) {
      args = java.util.Arrays.copyOf(args, args.length + 1);
      args[args.length - 1] = kv("error_code", errorCode);
    }

    switch (level) {
      case WARN -> log.warn(message, args);
      case DEBUG -> log.debug(message, args);
      case INFO -> log.info(message, args);
      case ERROR -> log.error(message, args);
      case TRACE -> log.trace(message, args);
    }
  }
}
