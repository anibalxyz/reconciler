package com.anibalxyz.server.config.modules.runtime;

import static com.anibalxyz.server.config.modules.runtime.MetricsConfig.METRICS_PATH;
import static net.logstash.logback.argument.StructuredArguments.kv;

import com.anibalxyz.server.config.modules.startup.StartupConfig;
import io.javalin.config.JavalinConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

public class AccessLogConfig implements StartupConfig {

  public static final String REQUEST_START_TIME_ATTR = "requestStartTime";
  private static final Logger log = LoggerFactory.getLogger("reconciler.access");

  @Override
  public void apply(JavalinConfig cfg) {
    cfg.routes.before(
        ctx -> {
          if (ctx.path().equals(METRICS_PATH)) return;
          if (ctx.path().startsWith("/webjars/")) return;
          ctx.attribute(REQUEST_START_TIME_ATTR, System.currentTimeMillis());
        });

    cfg.routes.after(
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
