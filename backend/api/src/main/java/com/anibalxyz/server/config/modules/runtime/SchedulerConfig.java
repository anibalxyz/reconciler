package com.anibalxyz.server.config.modules.runtime;

import static net.logstash.logback.argument.StructuredArguments.kv;

import com.anibalxyz.features.auth.application.RefreshTokenService;
import com.anibalxyz.server.config.modules.startup.StartupConfig;
import io.javalin.config.JavalinConfig;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// TODO: move to correspondent vertical slice
/**
 * Manages background tasks and periodic maintenance.
 *
 * <p>Initializes a {@link ScheduledExecutorService} for recurring tasks and ensures a graceful
 * shutdown when the server stops.
 */
public class SchedulerConfig implements StartupConfig {

  private static final Logger log = LoggerFactory.getLogger(SchedulerConfig.class);
  private final RefreshTokenService refreshTokenService;
  private ScheduledExecutorService scheduler;

  public SchedulerConfig(RefreshTokenService refreshTokenService) {
    this.refreshTokenService = refreshTokenService;
  }

  /** Initializes schedules and registers shutdown hooks. */
  @Override
  public void apply(JavalinConfig cfg) {
    scheduler = Executors.newSingleThreadScheduledExecutor();

    scheduler.scheduleAtFixedRate(
        () -> {
          int deletedCount = refreshTokenService.cleanupExpiredTokens();
          log.info("Finished scheduled refresh token cleanup", kv("deleted_count", deletedCount));
        },
        0,
        24,
        TimeUnit.HOURS);

    cfg.events.serverStopping(
        () -> {
          scheduler.shutdown();
          try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
              scheduler.shutdownNow();
            }
          } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
          }
        });
  }
}
