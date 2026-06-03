package com.anibalxyz.server;

import com.anibalxyz.features.auth.api.AuthApi;
import com.anibalxyz.features.auth.api.AuthController;
import com.anibalxyz.features.auth.api.AuthRoutes;
import com.anibalxyz.features.auth.api.JwtMiddleware;
import com.anibalxyz.features.auth.application.AuthService;
import com.anibalxyz.features.auth.application.JwtService;
import com.anibalxyz.features.auth.application.RefreshTokenService;
import com.anibalxyz.features.auth.domain.RefreshTokenRepository;
import com.anibalxyz.features.auth.infra.JpaRefreshTokenRepository;
import com.anibalxyz.features.system.api.SystemController;
import com.anibalxyz.features.system.api.SystemRoutes;
import com.anibalxyz.features.users.api.UserController;
import com.anibalxyz.features.users.api.UserRoutes;
import com.anibalxyz.features.users.application.UserService;
import com.anibalxyz.features.users.domain.UserRepository;
import com.anibalxyz.features.users.infra.JpaUserRepository;
import com.anibalxyz.persistence.EntityManagerProvider;
import com.anibalxyz.persistence.PersistenceManager;
import com.anibalxyz.server.config.environment.AppEnvironmentSource;
import com.anibalxyz.server.config.environment.ApplicationConfiguration;
import com.anibalxyz.server.config.modules.runtime.*;
import com.anibalxyz.server.config.modules.startup.ServerConfig;
import com.anibalxyz.server.config.modules.startup.SwaggerConfig;
import com.anibalxyz.server.context.JavalinContextEntityManagerProvider;
import io.javalin.micrometer.MicrometerPlugin;
import io.micrometer.prometheusmetrics.PrometheusConfig;
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry;
import java.time.Clock;

/**
 * A manual dependency injection container for the application.
 *
 * <p>This class is responsible for instantiating and wiring together the application's services,
 * controllers, and repositories. It follows the "Pure DI" pattern, where dependencies are created
 * in a single composition root, making the application's object graph explicit and easy to manage
 * without a DI framework.
 */
public final class DependencyContainer {

  private final PersistenceManager persistenceManager;
  private final LifecycleConfig lifecycleConfig;
  private final ExceptionsConfig exceptionsConfig;

  private final ServerConfig serverConfig;
  private final SwaggerConfig swaggerConfig;

  private final PrometheusMeterRegistry prometheusMeterRegistry;
  private final MicrometerPlugin micrometerPlugin;
  private final MetricsConfig metricsConfig;
  private final AccessLogConfig accessLogConfig;

  private final SystemRoutes systemRoutes;
  private final UserRoutes userRoutes;
  private final AuthRoutes authRoutes;
  private final SchedulerConfig schedulerConfig;

  public DependencyContainer(ApplicationConfiguration config, Clock clock) {
    AppEnvironmentSource env = config.env();
    EntityManagerProvider emProvider = new JavalinContextEntityManagerProvider();

    persistenceManager = new PersistenceManager(config.database());
    lifecycleConfig = new LifecycleConfig(persistenceManager);
    exceptionsConfig = new ExceptionsConfig();

    serverConfig = new ServerConfig(config.env());
    swaggerConfig = new SwaggerConfig(config.env());

    prometheusMeterRegistry = new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);
    micrometerPlugin =
        new MicrometerPlugin(
            micrometerPluginConfig -> micrometerPluginConfig.registry = prometheusMeterRegistry);
    metricsConfig = new MetricsConfig(prometheusMeterRegistry);
    accessLogConfig = new AccessLogConfig();

    UserRepository userRepository = new JpaUserRepository(emProvider);
    UserService userService = new UserService(env, userRepository);
    UserController userController = new UserController(userService);

    RefreshTokenRepository refreshTokenRepository = new JpaRefreshTokenRepository(emProvider);
    RefreshTokenService refreshTokenService = new RefreshTokenService(refreshTokenRepository);

    JwtService jwtService = new JwtService(env, clock);
    AuthService authService =
        new AuthService(env, clock, userService, jwtService, refreshTokenService);
    AuthApi authController = new AuthController(env, authService, refreshTokenService, clock);
    JwtMiddleware jwtMiddleware = new JwtMiddleware(jwtService);

    SystemController systemController = new SystemController(persistenceManager);

    systemRoutes = new SystemRoutes(systemController);
    userRoutes = new UserRoutes(userController);
    authRoutes = new AuthRoutes(authController, jwtMiddleware);
    schedulerConfig = new SchedulerConfig(refreshTokenService);
  }

  public PersistenceManager persistenceManager() {
    return persistenceManager;
  }

  public LifecycleConfig lifecycleConfig() {
    return lifecycleConfig;
  }

  public ExceptionsConfig exceptionsConfig() {
    return exceptionsConfig;
  }

  public ServerConfig serverConfig() {
    return serverConfig;
  }

  public SwaggerConfig swaggerConfig() {
    return swaggerConfig;
  }

  public MicrometerPlugin micrometerPlugin() {
    return micrometerPlugin;
  }

  public MetricsConfig metricsConfig() {
    return metricsConfig;
  }

  public AccessLogConfig accessLogConfig() {
    return accessLogConfig;
  }

  public SystemRoutes systemRoutes() {
    return systemRoutes;
  }

  public UserRoutes userRoutes() {
    return userRoutes;
  }

  public AuthRoutes authRoutes() {
    return authRoutes;
  }

  public SchedulerConfig schedulerConfig() {
    return schedulerConfig;
  }
}
