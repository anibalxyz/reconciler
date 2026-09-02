package com.anibalxyz.features.auth.infra;

import static net.logstash.logback.argument.StructuredArguments.kv;

import com.anibalxyz.features.auth.domain.RefreshTokenRepository;
import com.anibalxyz.server.config.modules.StartupConfig;
import io.javalin.config.JavalinConfig;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RefreshTokensCleanupScheduler implements StartupConfig {

  private static final Logger log = LoggerFactory.getLogger(RefreshTokensCleanupScheduler.class);
  private final RefreshTokenRepository refreshTokenRepository;
  private ScheduledExecutorService scheduler;

  public RefreshTokensCleanupScheduler(RefreshTokenRepository refreshTokenRepository) {
    this.refreshTokenRepository = refreshTokenRepository;
  }

  @Override
  public void apply(JavalinConfig cfg) {
    scheduler = Executors.newSingleThreadScheduledExecutor();

    scheduler.scheduleAtFixedRate(
        () -> {
          int deletedCount = refreshTokenRepository.deleteExpiredTokens();
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
