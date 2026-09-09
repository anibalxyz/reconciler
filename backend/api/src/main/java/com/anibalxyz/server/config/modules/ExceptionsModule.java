package com.anibalxyz.server.config.modules;

import static net.logstash.logback.argument.StructuredArguments.kv;

import com.anibalxyz.core.api.FailureSignal;
import com.anibalxyz.core.api.response.mappers.ErrorMapperResult;
import com.anibalxyz.core.primitives.LogEntry;
import com.anibalxyz.server.http.context.RequestContext;
import com.anibalxyz.server.http.mappers.ExceptionsMapper;
import com.anibalxyz.server.http.mappers.FeaturesErrorMapper;
import io.javalin.config.JavalinConfig;
import io.javalin.http.Context;
import io.javalin.http.HandlerType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.slf4j.event.Level;

public class ExceptionsModule implements JavalinModule {

  private static final Logger log = LoggerFactory.getLogger(ExceptionsModule.class);

  @Override
  public void apply(JavalinConfig cfg) {
    cfg.routes.exception(
        FailureSignal.class,
        (e, ctx) -> {
          ErrorMapperResult result = FeaturesErrorMapper.map(e.getError());
          String requestId = ctx.attribute(RequestContext.REQUEST_ID_KEY);
          MDC.put("status", String.valueOf(result.status()));
          emitLogEntry(result);

          ctx.status(result.status()).json(result.response().instance(requestId));
        });

    cfg.routes.exception(
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
    ErrorMapperResult result = ExceptionsMapper.map(e);
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

  private void emitLogEntry(ErrorMapperResult result) {
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
