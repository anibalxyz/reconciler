package com.anibalxyz.server.config.modules.runtime;

import com.anibalxyz.server.config.modules.startup.StartupConfig;
import io.javalin.config.JavalinConfig;
import io.javalin.micrometer.MicrometerPlugin;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.jvm.ClassLoaderMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmGcMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmMemoryMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmThreadMetrics;
import io.micrometer.core.instrument.binder.logging.LogbackMetrics;
import io.micrometer.core.instrument.binder.system.FileDescriptorMetrics;
import io.micrometer.core.instrument.binder.system.ProcessorMetrics;
import io.micrometer.core.instrument.binder.system.UptimeMetrics;
import io.micrometer.core.instrument.config.MeterFilter;
import io.micrometer.core.instrument.distribution.DistributionStatisticConfig;
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MetricsConfig implements StartupConfig {

  public static final String METRICS_PATH = "/internal/metrics";
  private static final Logger log = LoggerFactory.getLogger(MetricsConfig.class);
  private final MicrometerPlugin plugin;
  private final PrometheusMeterRegistry registry;

  public MetricsConfig(MicrometerPlugin plugin, PrometheusMeterRegistry registry) {
    this.plugin = plugin;
    this.registry = registry;
  }

  @SuppressWarnings("resource") // JvmGcMetrics already closed
  private static void bindMetrics(MeterRegistry registry) {
    new ClassLoaderMetrics().bindTo(registry);
    new JvmMemoryMetrics().bindTo(registry);
    new JvmGcMetrics().bindTo(registry);
    new JvmThreadMetrics().bindTo(registry);
    new ProcessorMetrics().bindTo(registry);
    new UptimeMetrics().bindTo(registry);
    new LogbackMetrics().bindTo(registry);
    new FileDescriptorMetrics().bindTo(registry);
  }

  private static void configCollapsed(MeterRegistry.Config config) {
    config.meterFilter(
        MeterFilter.forMeters(
            id -> "http.server.requests".equals(id.getName()),
            MeterFilter.replaceTagValues(
                "uri",
                uri -> {
                  if ("/openapi".equals(uri) || "/swagger".equals(uri)) {
                    return "/api-docs";
                  }
                  return uri;
                })));
  }

  private static void configPercentile(MeterRegistry.Config config) {
    config.meterFilter(
        new MeterFilter() {
          @Override
          public DistributionStatisticConfig configure(
              Meter.@NonNull Id id, @NonNull DistributionStatisticConfig config) {
            if (id.getName().equals("http.server.requests")) {
              return DistributionStatisticConfig.builder()
                  .percentilesHistogram(true)
                  .build()
                  .merge(config);
            }
            return config;
          }
        });
  }

  private static void configExcluded(MeterRegistry.Config config) {
    config.meterFilter(
        MeterFilter.deny(
            id -> {
              if (!"http.server.requests".equals(id.getName())) return false;
              String uri = id.getTag("uri");
              return uri != null
                  && ("/internal/metrics".equals(uri) || uri.startsWith("/webjars/"));
            }));
  }

  private static void registerEndpoint(JavalinConfig cfg, PrometheusMeterRegistry registry) {
    cfg.routes.get(
        METRICS_PATH,
        ctx -> {
          ctx.contentType("text/plain; version=0.0.4; charset=utf-8");
          ctx.result(registry.scrape());
        });
  }

  private void registerPlugin(JavalinConfig cfg) {
    cfg.registerPlugin(plugin);
  }

  @Override
  public void apply(JavalinConfig cfg) {
    configPercentile(registry.config());
    configCollapsed(registry.config());
    configExcluded(registry.config());
    bindMetrics(registry);
    registerEndpoint(cfg, registry);

    registerPlugin(cfg);

    log.info("Metrics endpoint registered at {}", METRICS_PATH);
  }

  // TODO: pending to be used
  public void close() {
    registry.close();
  }
}
