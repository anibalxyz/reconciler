package com.anibalxyz.server;

import static net.logstash.logback.argument.StructuredArguments.kv;

import com.anibalxyz.server.config.AppEnv;
import com.anibalxyz.server.config.ApplicationConfiguration;
import com.anibalxyz.server.config.settings.ClockSettings;
import com.anibalxyz.server.http.context.RequestContext;
import com.anibalxyz.server.persistence.PersistenceManager;
import io.javalin.Javalin;
import io.javalin.config.JavalinConfig;
import java.time.Clock;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Top-level assembly orchestrator for the application.
 *
 * <p>Created via one of the {@link #create} factories, wired by {@link DependencyContainer}, and
 * booted by {@code com.anibalxyz.Main}. {@code create} does not start the server; call {@link
 * #start(int)} explicitly.
 *
 * <p>Use {@link #create(ApplicationConfiguration)} for DEV/PROD and {@link
 * #create(ApplicationConfiguration, Clock)} for tests and custom assembly.
 */
public class Application {
  private static final Logger log = LoggerFactory.getLogger(Application.class);
  private final Javalin javalin;
  private final List<Runnable> shutdownTasks;
  private final PersistenceManager persistenceManager;
  private final ApplicationConfiguration config;

  private Application(
      Javalin javalin,
      List<Runnable> shutdownTasks,
      PersistenceManager persistenceManager,
      ApplicationConfiguration config) {

    this.javalin = javalin;
    this.shutdownTasks = shutdownTasks;
    this.persistenceManager = persistenceManager;
    this.config = config;
  }

  /**
   * Convenience factory for {@link AppEnv#DEV} and {@link AppEnv#PROD}: delegates to {@link
   * #create(ApplicationConfiguration, Clock)} with {@link #buildClock(AppEnv, ClockSettings)}}.
   */
  public static Application create(ApplicationConfiguration config) {
    return create(config, buildClock(config.appEnv(), config.clock()));
  }

  /**
   * Creates a fully assembled {@link Application} with an explicitly provided clock.
   *
   * <p>Builds the {@link DependencyContainer} for {@code config}, then configures a {@link Javalin}
   * server via {@link #setupJavalinConfig}.
   *
   * @param config application configuration
   * @param clock clock used by the application and its dependencies; must not be null
   * @return a fully assembled {@link Application}
   * @throws NullPointerException if {@code clock} is null
   */
  public static Application create(ApplicationConfiguration config, Clock clock) {
    Objects.requireNonNull(
        clock,
        "Clock must not be null. If you don't need a specific clock, use buildClock() instead.");

    DependencyContainer container = new DependencyContainer(config, clock);

    Consumer<JavalinConfig> javalinConfig = setupJavalinConfig(config, container);
    List<Runnable> shutdownTasks = setupShutdownTasks(container);

    Javalin server = Javalin.create(javalinConfig);

    return new Application(server, shutdownTasks, container.persistenceManager(), config);
  }

  /**
   * Resolves the {@link Clock} to use for a given environment: system clock in the configured
   * timezone for {@link AppEnv#PROD}, a fixed clock at {@code SYSTEM_TIME_OVERRIDE} if set, or the
   * system clock in America/Montevideo otherwise.
   *
   * @param appEnv the application environment
   * @param config the datetime configuration
   * @return the resolved clock
   */
  private static Clock buildClock(AppEnv appEnv, ClockSettings config) {
    if (appEnv == AppEnv.PROD) {
      return Clock.system(config.systemTimezone());
    }

    if (config.systemTimeOverride() != null) {
      return Clock.fixed(config.systemTimeOverride(), config.systemTimezone());
    }

    return Clock.system(ZoneId.of("America/Montevideo"));
  }

  /**
   * Builds the {@link JavalinConfig} setup function applying the startup modules owned by the
   * {@code container}: server settings, feature routes, integrations, and middlewares.
   *
   * @param config application configuration
   * @param container assembled dependency graph providing the startup modules
   * @return a consumer applying the startup sequence to a {@link JavalinConfig}
   */
  private static Consumer<JavalinConfig> setupJavalinConfig(
      ApplicationConfiguration config, DependencyContainer container) {
    return javalinConfig -> {
      container.serverConfig().apply(javalinConfig);

      if (config.featureFlags().SWAGGER()) {
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

  /**
   * Registers the application resources that require explicit cleanup upon shutdown.
   *
   * @param container the assembled dependency graph
   * @return <b>ordered</b> list of cleanup tasks to be executed sequentially during shutdown
   */
  private static List<Runnable> setupShutdownTasks(DependencyContainer container) {
    List<Runnable> shutdownTasks = new ArrayList<>();
    shutdownTasks.add(container.metricsConfig()::close);
    shutdownTasks.add(container.persistenceManager()::close);
    return shutdownTasks;
  }

  private static void tryShutdown(Runnable task) {
    try {
      task.run();
    } catch (Exception e) {
      log.error("Error shutting down task", e);
    }
  }

  /**
   * Starts the web server on the specified port.
   *
   * @param port The port to listen on.
   */
  public void start(int port) {
    log.info("Starting server on port {} [{} mode]", port, config.appEnv());
    javalin.start(port);
    log.info(
        "Server started successfully and is ready to accept connections on {}",
        kv("api_url", config.httpServer().apiUrl()));
  }

  /** Stops the web server and shuts down all registered resources gracefully. */
  public void shutdown() {
    log.info("Stopping Application...");
    javalin.stop();

    log.info("Shutting down Application resources...");
    shutdownTasks.forEach(Application::tryShutdown);

    log.info("Server shutdown completed successfully!");
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
}
