package com.anibalxyz.server;

import com.anibalxyz.features.auth.api.AuthRoutes;
import com.anibalxyz.features.system.api.SystemRoutes;
import com.anibalxyz.features.users.api.UserRoutes;
import com.anibalxyz.persistence.PersistenceManager;
import com.anibalxyz.server.config.AppEnv;
import com.anibalxyz.server.config.environment.AppEnvironmentSource;
import com.anibalxyz.server.config.environment.ApplicationConfiguration;
import com.anibalxyz.server.config.modules.runtime.ExceptionsConfig;
import com.anibalxyz.server.config.modules.runtime.LifecycleConfig;
import com.anibalxyz.server.config.modules.runtime.SchedulerConfig;
import com.anibalxyz.server.config.modules.startup.ServerConfig;
import com.anibalxyz.server.config.modules.startup.SwaggerConfig;
import com.anibalxyz.server.context.JavalinContextEntityManagerProvider;
import io.javalin.Javalin;
import io.javalin.config.JavalinConfig;
import java.time.Clock;
import java.time.ZoneId;
import java.util.function.Consumer;

/**
 * The main application class, acting as the Composition Root.
 *
 * <p>This class is responsible for initializing and wiring together all major components of the
 * application, including the web server (Javalin), persistence layer (PersistenceManager),
 * dependency container, and all configurations and routes. It provides factory methods to create an
 * instance tailored for different environments (e.g., test, development).
 */
public class Application {
  private final Javalin javalin;
  private final PersistenceManager persistenceManager;
  private final ApplicationConfiguration config;

  private Application(
      Javalin javalin, PersistenceManager persistenceManager, ApplicationConfiguration config) {
    this.javalin = javalin;
    this.persistenceManager = persistenceManager;
    this.config = config;
  }

  private static Clock buildClock(AppEnvironmentSource env) {
    if (env.APP_ENV() == AppEnv.PROD) {
      return Clock.system(env.SYSTEM_TIMEZONE());
    }

    if (env.SYSTEM_TIME_OVERRIDE() != null) {
      return Clock.fixed(env.SYSTEM_TIME_OVERRIDE(), env.SYSTEM_TIMEZONE());
    }

    return Clock.system(ZoneId.of("America/Montevideo"));
  }

  /**
   * A general factory method that creates an {@link Application} instance based on the environment
   * specified in the configuration.
   *
   * @param config The application configuration.
   * @return A new {@code Application} instance for the appropriate environment.
   * @throws IllegalStateException if the environment in the config is unknown.
   */
  public static Application create(ApplicationConfiguration config) {
    AppEnv appEnv = config.env().APP_ENV();

    if (appEnv == AppEnv.TEST) {
      throw new IllegalStateException(
          "For 'test' environment, directly use buildApplication() to specify feature-specific routes and configs.");
    }
    if (appEnv != AppEnv.DEV && appEnv != AppEnv.PROD) {
      throw new IllegalStateException("Unknown environment: " + appEnv);
    }

    // 1. Declare specific startup configurations for dev/prod
    Consumer<JavalinConfig> startupConfig =
        javalinConfig -> {
          new SwaggerConfig(javalinConfig, config.env()).apply();
        };

    // 2. Declare specific runtime configurations for dev/prod
    // TODO: move to a separate file, e.g. RedirectRoutes within features.common
    Consumer<DependencyContainer> runtimeConfigs =
        container -> {
          String openapiRedirect = appEnv == AppEnv.PROD ? "/openapi" : "/swagger";
          container.server().get("/", ctx -> ctx.redirect(openapiRedirect));
          container.server().get("/api", ctx -> ctx.redirect(openapiRedirect));
        };

    // 3. Declare specific route registries for dev/prod
    // TODO: migrate endpoint declarations to use apiBuilder()
    Consumer<DependencyContainer> routeRegistries =
        container -> {
          new SystemRoutes(container.server(), container.systemController()).register();
          new UserRoutes(container.server(), container.userController()).register();
          new AuthRoutes(container.server(), container.authController(), container.jwtMiddleware())
              .register();
          new SchedulerConfig(container.server(), container.refreshTokenService()).apply();
        };

    Clock clock = buildClock(config.env());
    return buildApplication(config, clock, startupConfig, runtimeConfigs, routeRegistries);
  }

  /**
   * The private, environment-agnostic "assembler" for the application.
   *
   * <p>This method is responsible for the core assembly logic: initializing common components and
   * then applying the specific configurations and routes provided to it. It does not make decisions
   * based on the application environment.
   *
   * @param config The application configuration.
   * @param customStartupConfigs A consumer for specific startup configurations (e.g., Swagger).
   * @param customRuntimeConfigs A consumer for specific runtime configurations.
   * @param customRoutesRegistries A consumer for registering specific routes.
   * @return A fully assembled {@code Application} instance.
   */
  public static Application buildApplication(
      ApplicationConfiguration config,
      Clock clock,
      Consumer<JavalinConfig> customStartupConfigs,
      Consumer<DependencyContainer> customRuntimeConfigs,
      Consumer<DependencyContainer> customRoutesRegistries) {
    clock = clock != null ? clock : buildClock(config.env());

    PersistenceManager persistenceManager = new PersistenceManager(config.database());

    Consumer<JavalinConfig> finalStartupConfig =
        javalinConfig -> {
          new ServerConfig(javalinConfig, config.env()).apply();
          if (customStartupConfigs != null) customStartupConfigs.accept(javalinConfig);
        };
    Javalin server = Javalin.create(finalStartupConfig);

    DependencyContainer container =
        new DependencyContainer(
            server,
            config.env(),
            new JavalinContextEntityManagerProvider(),
            persistenceManager,
            clock);

    new LifecycleConfig(server, persistenceManager).apply();
    new ExceptionsConfig(server).apply();

    if (customRuntimeConfigs != null) customRuntimeConfigs.accept(container);
    if (customRoutesRegistries != null) customRoutesRegistries.accept(container);

    return new Application(server, persistenceManager, config);
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
    javalin.start(port);
  }

  /** Stops the web server and shuts down the persistence layer gracefully. */
  public void stop() {
    javalin.stop();
    persistenceManager.shutdown();
  }
}
