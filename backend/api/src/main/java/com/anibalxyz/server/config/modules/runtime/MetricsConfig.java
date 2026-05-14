package com.anibalxyz.server.config.modules.runtime;

import io.javalin.Javalin;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.binder.jvm.ClassLoaderMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmGcMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmMemoryMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmThreadMetrics;
import io.micrometer.core.instrument.binder.logging.LogbackMetrics;
import io.micrometer.core.instrument.binder.system.FileDescriptorMetrics;
import io.micrometer.core.instrument.binder.system.ProcessorMetrics;
import io.micrometer.core.instrument.binder.system.UptimeMetrics;
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MetricsConfig extends RuntimeConfig {

  public static final String METRICS_PATH = "/internal/metrics";
  public static final String METRICS_TIMER_ATTR = "metrics_timer";
  private static final Logger log = LoggerFactory.getLogger(MetricsConfig.class);
  private final PrometheusMeterRegistry registry;

  public MetricsConfig(Javalin server, PrometheusMeterRegistry registry) {
    super(server);
    this.registry = registry;
  }

  @Override
  @SuppressWarnings("resource") // JvmGcMetrics already closed
  public void apply() {
    new ClassLoaderMetrics().bindTo(registry);
    new JvmMemoryMetrics().bindTo(registry);
    new JvmGcMetrics().bindTo(registry);
    new JvmThreadMetrics().bindTo(registry);
    new ProcessorMetrics().bindTo(registry);
    new UptimeMetrics().bindTo(registry);
    new LogbackMetrics().bindTo(registry);
    new FileDescriptorMetrics().bindTo(registry);

    server.before(
        ctx -> {
          if (ctx.path().equals(METRICS_PATH)) return;
          ctx.attribute(METRICS_TIMER_ATTR, Timer.start(registry));
        });

    server.after(
        ctx -> {
          Timer.Sample sample = ctx.attribute(METRICS_TIMER_ATTR);
          if (sample == null) return;

          var path = ctx.endpointHandlerPath();
          if (path.startsWith("No handler matched")) path = "/unmatched";

          sample.stop(
              Timer.builder("http_server_requests")
                  .description("HTTP request duration")
                  .tag("method", ctx.method().name())
                  .tag("path", path)
                  .tag("status", String.valueOf(ctx.statusCode()))
                  .register(registry));
        });

    server.get(
        METRICS_PATH,
        ctx -> {
          ctx.contentType("text/plain; version=0.0.4; charset=utf-8");
          ctx.result(registry.scrape());
        });

    log.info("Metrics endpoint registered at {}", METRICS_PATH);
  }

  // TODO: pending to be used
  public void close() {
    registry.close();
  }
}
