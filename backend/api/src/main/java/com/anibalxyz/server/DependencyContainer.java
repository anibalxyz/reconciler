package com.anibalxyz.server;

import com.anibalxyz.features.auth.api.AuthApi;
import com.anibalxyz.features.auth.api.AuthController;
import com.anibalxyz.features.auth.api.AuthRoutes;
import com.anibalxyz.features.auth.api.JwtMiddleware;
import com.anibalxyz.features.auth.application.*;
import com.anibalxyz.features.auth.domain.MaintenancePolicy;
import com.anibalxyz.features.auth.domain.RefreshTokenRepository;
import com.anibalxyz.features.auth.infra.JpaRefreshTokenRepository;
import com.anibalxyz.features.auth.infra.RefreshTokensCleanupScheduler;
import com.anibalxyz.features.system.api.SystemController;
import com.anibalxyz.features.system.api.SystemRoutes;
import com.anibalxyz.features.users.api.UserRoutes;
import com.anibalxyz.features.users.api.handlers.*;
import com.anibalxyz.features.users.application.*;
import com.anibalxyz.features.users.domain.UserRepository;
import com.anibalxyz.features.users.infra.JpaUserRepository;
import com.anibalxyz.persistence.EntityManagerProvider;
import com.anibalxyz.persistence.PersistenceManager;
import com.anibalxyz.server.config.environment.AppEnvironmentSource;
import com.anibalxyz.server.config.environment.ApplicationConfiguration;
import com.anibalxyz.server.config.modules.AccessLogConfig;
import com.anibalxyz.server.config.modules.ExceptionsConfig;
import com.anibalxyz.server.config.modules.LifecycleConfig;
import com.anibalxyz.server.config.modules.MetricsConfig;
import com.anibalxyz.server.config.modules.ServerConfig;
import com.anibalxyz.server.config.modules.SwaggerConfig;
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

  private final LifecycleConfig lifecycleConfig;
  private final ExceptionsConfig exceptionsConfig;
  private final AccessLogConfig accessLogConfig;
  private final MetricsConfig metricsConfig;

  private final JwtMiddleware jwtMiddleware;

  private final SystemRoutes systemRoutes;
  private final UserRoutes userRoutes;
  private final AuthRoutes authRoutes;

  private final RefreshTokensCleanupScheduler refreshTokensCleanupScheduler;

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
    MicrometerPlugin micrometerPlugin =
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

    // 4. Services / Use Cases / Policies
    GetAllUsers getAllUsers = new GetAllUsers(userRepository);
    GetUserByEmail getUserByEmail = new GetUserByEmail(userRepository);
    GetUserById getUserById = new GetUserById(userRepository);
    CreateUser createUser = new CreateUser(env, userRepository);
    UpdateUserById updateUserById = new UpdateUserById(env, userRepository);
    DeleteUserById deleteUserById = new DeleteUserById(userRepository);

    MaintenancePolicy maintenancePolicy = new MaintenancePolicy();

    RefreshTokenService refreshTokenService = new RefreshTokenService(refreshTokenRepository);
    JwtService jwtService = new JwtService(env, clock);

    AuthenticateUser authenticateUser =
        new AuthenticateUser(
            env, clock, maintenancePolicy, getUserByEmail, jwtService, refreshTokenService);
    RefreshTokens refreshTokens =
        new RefreshTokens(env, clock, maintenancePolicy, jwtService, refreshTokenService);

    // 5. Handlers and Middlewares
    // Handlers
    GetAllUsersHandler getAllUsersHandler = new GetAllUsersHandler(getAllUsers);
    GetUserByIdHandler getUserByIdHandler = new GetUserByIdHandler(getUserById);
    CreateUserHandler createUserHandler = new CreateUserHandler(createUser);
    UpdateUserByIdHandler updateUserByIdHandler = new UpdateUserByIdHandler(updateUserById);
    DeleteUserByIdHandler deleteUserByIdHandler = new DeleteUserByIdHandler(deleteUserById);

    AuthApi authController =
        new AuthController(env, clock, authenticateUser, refreshTokens, refreshTokenService);
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
    refreshTokensCleanupScheduler = new RefreshTokensCleanupScheduler(refreshTokenService);
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

  public RefreshTokensCleanupScheduler schedulerConfig() {
    return refreshTokensCleanupScheduler;
  }
}
