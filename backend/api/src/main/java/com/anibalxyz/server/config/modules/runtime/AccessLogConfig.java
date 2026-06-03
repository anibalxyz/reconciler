package com.anibalxyz.server.config.modules.runtime;

import static com.anibalxyz.server.config.modules.runtime.MetricsConfig.METRICS_PATH;
import static net.logstash.logback.argument.StructuredArguments.kv;

import io.javalin.Javalin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

public class AccessLogConfig implements RuntimeConfig {

  public static final String REQUEST_START_TIME_ATTR = "requestStartTime";
  private static final Logger log = LoggerFactory.getLogger("reconciler.access");

  @Override
  public void apply(Javalin server) {
    server.before(
        ctx -> {
          if (ctx.path().equals(METRICS_PATH)) return;
          if (ctx.path().startsWith("/webjars/")) return;
          ctx.attribute(REQUEST_START_TIME_ATTR, System.currentTimeMillis());
        });

    server.after(
        ctx -> {
          Long startTime = ctx.attribute(REQUEST_START_TIME_ATTR);
          if (startTime == null) return;

          MDC.put("status", String.valueOf(ctx.statusCode()));
          long duration = System.currentTimeMillis() - startTime;
          log.info(
              "{} {} -> {} [{}ms]",
              ctx.method(),
              ctx.path(),
              ctx.statusCode(),
              duration,
              kv("duration", duration));
        });
  }
}
