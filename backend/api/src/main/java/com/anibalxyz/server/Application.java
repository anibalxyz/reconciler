package com.anibalxyz.server;

import com.anibalxyz.persistence.PersistenceManager;
import com.anibalxyz.server.config.AppConfig;
import com.anibalxyz.server.config.ExceptionsConfig;
import com.anibalxyz.server.config.InitConfig;
import com.anibalxyz.server.config.LifeCycleConfig;
import com.anibalxyz.server.config.ServerConfig;
import com.anibalxyz.server.context.JavalinContextEntityManagerProvider;
import com.anibalxyz.server.routes.Router;
import com.anibalxyz.server.routes.SystemRoutes;
import com.anibalxyz.users.api.UserRoutes;
import io.javalin.Javalin;
import java.util.List;

public class Application {

  private final Javalin javalin;
  private final PersistenceManager persistenceManager;
  private final AppConfig config;

  private Application(Javalin javalin, PersistenceManager persistenceManager, AppConfig config) {
    this.javalin = javalin;
    this.persistenceManager = persistenceManager;
    this.config = config;
  }

  public static Application createForTest(AppConfig config) {
    PersistenceManager persistenceManager = new PersistenceManager(config.database());

    Javalin server = Javalin.create(javalinConfig -> new InitConfig(javalinConfig, config).apply());

    DependencyContainer container =
        new DependencyContainer(new JavalinContextEntityManagerProvider());

    List<Router> routers =
        List.of(
            new UserRoutes(server, container.getUserController()),
            new SystemRoutes(server, persistenceManager));
    List<ServerConfig> serverConfigs =
        List.of(new LifeCycleConfig(server, persistenceManager), new ExceptionsConfig(server));

    routers.forEach(Router::register);
    serverConfigs.forEach(ServerConfig::apply);

    return new Application(server, persistenceManager, config);
  }

  // TODO: also used in "production", for the moment both are the same
  // I think it breaks YAGNI, i'll change it later if it doesn't change anything
  public static Application createForDevelopment(AppConfig config) {
    PersistenceManager persistenceManager = new PersistenceManager(config.database());

    Javalin server = Javalin.create(javalinConfig -> new InitConfig(javalinConfig, config).apply());

    DependencyContainer container =
        new DependencyContainer(new JavalinContextEntityManagerProvider());

    List<Router> routers =
        List.of(
            new UserRoutes(server, container.getUserController()),
            new SystemRoutes(server, persistenceManager));
    List<ServerConfig> serverConfigs =
        List.of(new LifeCycleConfig(server, persistenceManager), new ExceptionsConfig(server));

    routers.forEach(Router::register);
    serverConfigs.forEach(ServerConfig::apply);

    return new Application(server, persistenceManager, config);
  }

  public static Application create(AppConfig config) {
    if (config.env().equals("development")) {
      return createForDevelopment(config);
    }
    if (config.env().equals("test")) {
      return createForTest(config);
    }
    throw new IllegalStateException("Unknown environment: " + config.env());
  }

  public Javalin javalin() {
    return javalin;
  }

  public PersistenceManager persistenceManager() {
    return persistenceManager;
  }

  public AppConfig config() {
    return config;
  }

  public void start(int port) {
    javalin.start(port);
  }

  public void stop() {
    javalin.stop();
    persistenceManager.shutdown();
  }
}
