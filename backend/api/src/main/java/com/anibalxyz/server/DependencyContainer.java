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
import com.anibalxyz.features.users.api.UserRoutes;
import com.anibalxyz.features.users.api.handlers.*;
import com.anibalxyz.features.users.api.openapi.*;
import com.anibalxyz.features.users.application.*;
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

  private final ServerConfig serverConfig;
  private final SwaggerConfig swaggerConfig;
  private final MicrometerPlugin micrometerPlugin;

  private final LifecycleConfig lifecycleConfig;
  private final ExceptionsConfig exceptionsConfig;
  private final AccessLogConfig accessLogConfig;
  private final MetricsConfig metricsConfig;

  private final JwtMiddleware jwtMiddleware;

  private final SystemRoutes systemRoutes;
  private final UserRoutes userRoutes;
  private final AuthRoutes authRoutes;

  private final SchedulerConfig schedulerConfig;

  public DependencyContainer(ApplicationConfiguration config, Clock clock) {
    // 1. Infrastructure
    AppEnvironmentSource env = config.env();
    EntityManagerProvider emProvider = new JavalinContextEntityManagerProvider();
    var prometheusMeterRegistry = new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);
    persistenceManager = new PersistenceManager(config.database());

    // 2. Configurations
    // Startup Configurations
    serverConfig = new ServerConfig(config.env());
    swaggerConfig = new SwaggerConfig(config.env());
    micrometerPlugin =
        new MicrometerPlugin(
            micrometerPluginConfig -> micrometerPluginConfig.registry = prometheusMeterRegistry);

    // Runtime Configurations
    lifecycleConfig = new LifecycleConfig(persistenceManager);
    exceptionsConfig = new ExceptionsConfig();
    accessLogConfig = new AccessLogConfig();
    metricsConfig = new MetricsConfig(micrometerPlugin, prometheusMeterRegistry);

    // 3. Repositories
    UserRepository userRepository = new JpaUserRepository(emProvider);
    RefreshTokenRepository refreshTokenRepository = new JpaRefreshTokenRepository(emProvider);

    // 4. Services
    // Use Cases (temporary)
    GetAllUsers getAllUsers = new GetAllUsers(userRepository);
    GetUserByEmail getUserByEmail = new GetUserByEmail(userRepository);
    GetUserById getUserById = new GetUserById(userRepository);
    CreateUser createUser = new CreateUser(env, userRepository);
    UpdateUserById updateUserById = new UpdateUserById(env, userRepository);
    DeleteUserById deleteUserById = new DeleteUserById(userRepository);

    RefreshTokenService refreshTokenService = new RefreshTokenService(refreshTokenRepository);
    JwtService jwtService = new JwtService(env, clock);
    AuthService authService =
        new AuthService(env, clock, getUserByEmail, jwtService, refreshTokenService);

    // 5. Handlers and Middlewares
    // Handlers
    GetAllUsersHandler getAllUsersHandler = new GetAllUsersHandler(getAllUsers);
    GetUserByIdHandler getUserByIdHandler = new GetUserByIdHandler(getUserById);
    CreateUserHandler createUserHandler = new CreateUserHandler(createUser);
    UpdateUserByIdHandler updateUserByIdHandler = new UpdateUserByIdHandler(updateUserById);
    DeleteUserByIdHandler deleteUserByIdHandler = new DeleteUserByIdHandler(deleteUserById);

    AuthApi authController = new AuthController(env, authService, refreshTokenService, clock);
    SystemController systemController = new SystemController(persistenceManager);

    // Middlewares
    jwtMiddleware = new JwtMiddleware(jwtService);

    // 6. Routes and Events
    // Routes
    systemRoutes = new SystemRoutes(systemController);
    userRoutes =
        new UserRoutes(
            getAllUsersHandler,
            getUserByIdHandler,
            createUserHandler,
            updateUserByIdHandler,
            deleteUserByIdHandler);
    authRoutes = new AuthRoutes(authController);

    // Events
    schedulerConfig = new SchedulerConfig(refreshTokenService);
  }

  public PersistenceManager persistenceManager() {
    return persistenceManager;
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

  public LifecycleConfig lifecycleConfig() {
    return lifecycleConfig;
  }

  public ExceptionsConfig exceptionsConfig() {
    return exceptionsConfig;
  }

  public AccessLogConfig accessLogConfig() {
    return accessLogConfig;
  }

  public MetricsConfig metricsConfig() {
    return metricsConfig;
  }

  public JwtMiddleware jwtMiddleware() {
    return jwtMiddleware;
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
