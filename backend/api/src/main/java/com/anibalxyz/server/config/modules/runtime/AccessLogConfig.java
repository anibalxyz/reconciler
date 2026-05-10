package com.anibalxyz.server.config.modules.runtime;

import static net.logstash.logback.argument.StructuredArguments.kv;

import io.javalin.Javalin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

public class AccessLogConfig extends RuntimeConfig {

  public static final String REQUEST_START_TIME_ATTR = "requestStartTime";
  private static final Logger log = LoggerFactory.getLogger("reconciler.access");

  public AccessLogConfig(Javalin server) {
    super(server);
  }

  @Override
  public void apply() {
    server.before(ctx -> ctx.attribute(REQUEST_START_TIME_ATTR, System.currentTimeMillis()));

    server.after(
        ctx -> {
          MDC.put("status", String.valueOf(ctx.statusCode()));
          Long startTime = ctx.attribute(REQUEST_START_TIME_ATTR);
          if (startTime == null) return;

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
