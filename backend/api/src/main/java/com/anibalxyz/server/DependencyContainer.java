package com.anibalxyz.server;

import com.anibalxyz.features.auth.api.AuthApi;
import com.anibalxyz.features.auth.api.AuthController;
import com.anibalxyz.features.auth.api.JwtMiddleware;
import com.anibalxyz.features.auth.application.AuthService;
import com.anibalxyz.features.auth.application.JwtService;
import com.anibalxyz.features.auth.application.RefreshTokenService;
import com.anibalxyz.features.auth.domain.RefreshTokenRepository;
import com.anibalxyz.features.auth.infra.JpaRefreshTokenRepository;
import com.anibalxyz.features.system.api.SystemController;
import com.anibalxyz.features.users.api.UserController;
import com.anibalxyz.features.users.application.UserService;
import com.anibalxyz.features.users.domain.UserRepository;
import com.anibalxyz.features.users.infra.JpaUserRepository;
import com.anibalxyz.persistence.EntityManagerProvider;
import com.anibalxyz.persistence.PersistenceManager;
import com.anibalxyz.server.config.environment.AppEnvironmentSource;
import io.javalin.Javalin;
import java.time.Clock;

/**
 * A manual dependency injection container for the application.
 *
 * <p>This class is responsible for instantiating and wiring together the application's services,
 * controllers, and repositories. It follows the "Pure DI" pattern, where dependencies are created
 * in a single composition root, making the application's object graph explicit and easy to manage
 * without a DI framework.
 */
public class DependencyContainer {
  private final Javalin server;
  private final UserController userController;
  private final AuthApi authController;
  private final SystemController systemController;
  private final RefreshTokenService refreshTokenService;

  private final JwtMiddleware jwtMiddleware;

  public DependencyContainer(
      Javalin server,
      AppEnvironmentSource env,
      EntityManagerProvider emProvider,
      PersistenceManager persistenceManager,
      Clock clock) {
    this.server = server;
    UserRepository userRepository = new JpaUserRepository(emProvider);
    UserService userService = new UserService(env, userRepository);
    userController = new UserController(userService);

    RefreshTokenRepository refreshTokenRepository = new JpaRefreshTokenRepository(emProvider);
    refreshTokenService = new RefreshTokenService(refreshTokenRepository);

    JwtService jwtService = new JwtService(env, clock);
    AuthService authService =
        new AuthService(env, clock, userService, jwtService, refreshTokenService);
    authController = new AuthController(env, authService, refreshTokenService, clock);
    jwtMiddleware = new JwtMiddleware(jwtService);

    systemController = new SystemController(persistenceManager);
  }

  public UserController userController() {
    return userController;
  }

  public AuthApi authController() {
    return authController;
  }

  public SystemController systemController() {
    return systemController;
  }

  public JwtMiddleware jwtMiddleware() {
    return jwtMiddleware;
  }

  public RefreshTokenService refreshTokenService() {
    return refreshTokenService;
  }

  public Javalin server() {
    return server;
  }
}
