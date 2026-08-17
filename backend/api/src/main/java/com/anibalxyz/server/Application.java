package com.anibalxyz.server;

import static net.logstash.logback.argument.StructuredArguments.kv;

import com.anibalxyz.persistence.PersistenceManager;
import com.anibalxyz.server.config.AppEnv;
import com.anibalxyz.server.config.environment.AppEnvironmentSource;
import com.anibalxyz.server.config.environment.ApplicationConfiguration;
import com.anibalxyz.server.context.RequestContext;
import io.javalin.Javalin;
import io.javalin.config.JavalinConfig;
import java.time.Clock;
import java.time.ZoneId;
import java.util.Objects;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Top-level assembly orchestrator for the application.
 *
 * <p>Creates the {@link DependencyContainer}, configures and starts the Javalin server, and wires
 * all configs, plugins, routes, and middlewares. Provides {@link #create} as a convenience for
 * DEV/PROD and {@link #create} as a low-level entry point for testing and custom assembly.
 */
public class Application {
  private static final Logger log = LoggerFactory.getLogger(Application.class);
  private final Javalin javalin;
  private final PersistenceManager persistenceManager;
  private final ApplicationConfiguration config;

  private Application(
      Javalin javalin, PersistenceManager persistenceManager, ApplicationConfiguration config) {
    this.javalin = javalin;
    this.persistenceManager = persistenceManager;
    this.config = config;
  }

  /**
   * Convenience factory for {@link AppEnv#DEV} and {@link AppEnv#PROD} environments.
   *
   * @param config application configuration
   * @return a fully assembled {@link Application}
   */
  public static Application create(ApplicationConfiguration config) {
    return create(config, buildClock(config.env()));
  }

  /**
   * Low-level assembly method that wires the full application.
   *
   * <p>TODO: Document this method.
   */
  public static Application create(ApplicationConfiguration config, Clock clock) {
    Objects.requireNonNull(
        clock,
        "Clock must not be null. If you don't need a specific clock, use buildClock() instead.");

    DependencyContainer container = new DependencyContainer(config, clock);

    Consumer<JavalinConfig> javalinConfig = setupJavalinConfig(config, container);
    Javalin server = Javalin.create(javalinConfig);

    return new Application(server, container.persistenceManager(), config);
  }

  private static Consumer<JavalinConfig> setupJavalinConfig(
      ApplicationConfiguration config, DependencyContainer container) {
    return javalinConfig -> {
      container.serverConfig().apply(javalinConfig);

      if (config.env().SWAGGER_ENABLED()) {
        container.swaggerConfig().apply(javalinConfig);
        container.systemRoutes().applyRedirects(javalinConfig);
      }

      container.systemRoutes().apply(javalinConfig);
      container.userRoutes().apply(javalinConfig);
      container.authRoutes().apply(javalinConfig);

      container.accessLogConfig().apply(javalinConfig);
      container.metricsConfig().apply(javalinConfig);
      container.schedulerConfig().apply(javalinConfig);

      container.jwtMiddleware().apply(javalinConfig);

      container.lifecycleConfig().apply(javalinConfig);
      container.exceptionsConfig().apply(javalinConfig);

      // TODO: Refactor request lifecycle management.
      //       Current temporal fix: RequestContext.clear() is moved here to ensure it's the
      //       absolute last operation in the 'after' hook chain. Scattered 'before/after' hooks
      //       across modules (Lifecycle, Metrics, AccessLog) make execution order
      //       non-deterministic.
      //       Planned improvement: Centralize all hooks into a single Orchestrator/Config that
      //       accepts Consumers from each module.
      javalinConfig.routes.after(ctx -> RequestContext.clear());
    };
  }

  public static Clock buildClock(AppEnvironmentSource env) {
    if (env.APP_ENV() == AppEnv.PROD) {
      return Clock.system(env.SYSTEM_TIMEZONE());
    }

    if (env.SYSTEM_TIME_OVERRIDE() != null) {
      return Clock.fixed(env.SYSTEM_TIME_OVERRIDE(), env.SYSTEM_TIMEZONE());
    }

    return Clock.system(ZoneId.of("America/Montevideo"));
  }

  public Javalin javalin() {
    return javalin;
  }

  public PersistenceManager persistenceManager() {
    return persistenceManager;
  }

  public ApplicationConfiguration config() {
    return config;
  }

  /**
   * Starts the web server on the specified port.
   *
   * @param port The port to listen on.
   */
  public void start(int port) {
    log.info("Starting server on port {} [{} mode]", port, config.env().APP_ENV());
    javalin.start(port);
    log.info(
        "Server started successfully and is ready to accept connections on {}",
        kv("api_url", config.env().API_URL()));
  }

  /** Stops the web server and shuts down the persistence layer gracefully. */
  public void stop() {
    javalin.stop();
    persistenceManager.shutdown();
  }
}
