package com.anibalxyz.server;

import static net.logstash.logback.argument.StructuredArguments.kv;

import com.anibalxyz.persistence.PersistenceManager;
import com.anibalxyz.server.config.AppEnv;
import com.anibalxyz.server.config.environment.AppEnvironmentSource;
import com.anibalxyz.server.config.environment.ApplicationConfiguration;
import com.anibalxyz.server.config.modules.runtime.*;
import com.anibalxyz.server.config.modules.startup.SwaggerConfig;
import com.anibalxyz.server.context.RequestContext;
import io.javalin.Javalin;
import io.javalin.config.JavalinConfig;
import java.time.Clock;
import java.time.ZoneId;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Top-level assembly orchestrator for the application.
 *
 * <p>Creates the {@link DependencyContainer}, configures and starts the Javalin server, and wires
 * all configs, plugins, routes, and middlewares. Provides {@link #create} as a convenience for
 * DEV/PROD and {@link #buildApplication} as a low-level entry point for testing and custom
 * assembly.
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

  public static Clock buildClock(AppEnvironmentSource env) {
    if (env.APP_ENV() == AppEnv.PROD) {
      return Clock.system(env.SYSTEM_TIMEZONE());
    }

    if (env.SYSTEM_TIME_OVERRIDE() != null) {
      return Clock.fixed(env.SYSTEM_TIME_OVERRIDE(), env.SYSTEM_TIMEZONE());
    }

    return Clock.system(ZoneId.of("America/Montevideo"));
  }

  /**
   * Convenience factory for {@link AppEnv#DEV} and {@link AppEnv#PROD} environments.
   *
   * <p>Wires all startup configs, plugins, runtime configs, routes, middlewares, events, etc.
   *
   * @param config application configuration
   * @return a fully assembled {@code Application}
   * @throws IllegalStateException if the environment is {@link AppEnv#TEST} or unknown
   */
  public static Application create(ApplicationConfiguration config) {
    Clock clock = buildClock(config.env());

    AppEnv appEnv = config.env().APP_ENV();

    if (appEnv == AppEnv.TEST) {
      throw new IllegalStateException(
          "For 'test' environment, directly use buildApplication() to specify feature-specific routes and configs.");
    }
    if (appEnv != AppEnv.DEV && appEnv != AppEnv.PROD) {
      throw new IllegalStateException("Unknown environment: " + appEnv);
    }

    // 1. Declare specific startup configurations for dev/prod
    BiConsumer<JavalinConfig, DependencyContainer> startupConfig =
        (javalinConfig, container) -> {
          if (config.env().SWAGGER_ENABLED()) {
            container.swaggerConfig().apply(javalinConfig);
          }
          javalinConfig.registerPlugin(container.micrometerPlugin());
        };

    // 2. Declare specific runtime configurations for dev/prod
    // TODO: move to a separate file, e.g. RedirectRoutes within features.common
    BiConsumer<Javalin, DependencyContainer> runtimeConfigs =
        (server, container) -> {
          if (config.env().SWAGGER_ENABLED()) {
            server.get("/", ctx -> ctx.redirect("/swagger"));
            server.get("/api", ctx -> ctx.redirect("/swagger"));

            server.after(
                "/swagger", ctx -> SwaggerConfig.swaggerPatch(ctx, config.env().APP_ENV()));
          }

          container.accessLogConfig().apply(server);
          container.metricsConfig().apply(server);
        };

    // 3. Declare specific route registries for dev/prod
    // TODO: migrate endpoint declarations to use apiBuilder()
    BiConsumer<Javalin, DependencyContainer> routeRegistries =
        (server, container) -> {
          container.systemRoutes().register(server);
          container.userRoutes().register(server);
          container.authRoutes().register(server);
          container.schedulerConfig().apply(server);
        };

    return buildApplication(config, clock, startupConfig, runtimeConfigs, routeRegistries);
  }

  /**
   * Low-level assembly method that wires the full application.
   *
   * <p>Creates the {@link DependencyContainer}, configures the server via three extension points,
   * and returns a ready-to-start {@link Application}. Base configs (server, lifecycle, exceptions)
   * are always applied; the callbacks add environment-specific behavior.
   *
   * @param config application configuration
   * @param clock clock to use (must not be null)
   * @param startupConfigs applied inside the {@code JavalinConfig} lambda, before server creation
   * @param runtimeConfigs applied after server creation, for runtime wiring
   * @param routeRegistries applied after server creation, for route registration
   * @return a fully assembled {@code Application}
   */
  public static Application buildApplication(
      ApplicationConfiguration config,
      Clock clock,
      BiConsumer<JavalinConfig, DependencyContainer> startupConfigs,
      BiConsumer<Javalin, DependencyContainer> runtimeConfigs,
      BiConsumer<Javalin, DependencyContainer> routeRegistries) {
    Objects.requireNonNull(
        clock,
        "Clock must not be null. If you don't need a specific clock, use buildClock() instead.");

    DependencyContainer container = new DependencyContainer(config, clock);

    Consumer<JavalinConfig> finalStartupConfig =
        javalinConfig -> {
          container.serverConfig().apply(javalinConfig);
          if (startupConfigs != null) startupConfigs.accept(javalinConfig, container);
        };
    Javalin server = Javalin.create(finalStartupConfig);

    container.lifecycleConfig().apply(server);
    container.exceptionsConfig().apply(server);
    // TODO: Refactor request lifecycle management.
    //       Current temporal fix: RequestContext.clear() is moved here to ensure it's the absolute
    //       last operation in the 'after' hook chain.
    //       Scattered 'before/after' hooks across modules (Lifecycle, Metrics, AccessLog) make
    //       execution order non-deterministic.
    //       Planned improvement: Centralize all hooks into a single Orchestrator/Config that
    //       accepts Consumers from each module.
    server.after(ctx -> RequestContext.clear());

    if (runtimeConfigs != null) runtimeConfigs.accept(server, container);
    if (routeRegistries != null) routeRegistries.accept(server, container);

    return new Application(server, container.persistenceManager(), config);
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
