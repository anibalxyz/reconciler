package com.anibalxyz.server;

import com.anibalxyz.persistence.DatabaseVariables;
import com.anibalxyz.persistence.PersistenceManager;
import com.anibalxyz.server.routes.SystemRoutes;
import com.anibalxyz.users.api.UserController;
import com.anibalxyz.users.api.UserRoutes;
import com.anibalxyz.users.application.UserService;
import com.anibalxyz.users.infra.JpaUserRepository;
import com.anibalxyz.persistence.EntityManagerProvider;
import com.fasterxml.jackson.databind.JsonMappingException;
import io.javalin.Javalin;
import io.javalin.http.BadRequestResponse;

import jakarta.persistence.EntityManager;

import java.util.Map;

public class Router {
  private static Javalin server;

  public static void init(int port) {
    server = Javalin.create(Config::apply);
    loadExceptions();

    DatabaseVariables dbVars = DatabaseVariables.fromEnv();

    PersistenceManager persistenceManager = new PersistenceManager(dbVars);

    EntityManagerProvider emProvider = new JavalinContextEntityManagerProvider();
    JpaUserRepository userRepository = new JpaUserRepository(emProvider);
    UserService userService = new UserService(userRepository);
    UserController userController = new UserController(userService);

    setEntityManagerLifecycle(persistenceManager);
    new UserRoutes(server, userController).register();
    new SystemRoutes(server, persistenceManager).register();
    server.start(port);

    Runtime.getRuntime().addShutdownHook(new Thread(persistenceManager::shutdown));
  }

  private static void loadExceptions() {
    boolean isDev = System.getenv("APP_ENV").equals("development");
    server.exception(
        BadRequestResponse.class,
        (e, ctx) -> {
          ctx.status(400);
          if (isDev) e.printStackTrace();
          ctx.json(Map.of("error", e.getMessage()));
        });
    server.exception(
        Exception.class,
        (e, ctx) -> {
          ctx.status(500);
          if (isDev) e.printStackTrace();
          ctx.json(Map.of("error", isDev ? e.getMessage() : "Internal Server Error"));
        });
    server.exception(
        JsonMappingException.class,
        (e, ctx) -> {
          ctx.status(400).json(Map.of("error", "Malformed or empty JSON"));
        });
  }

  private static void setEntityManagerLifecycle(PersistenceManager persistenceManager) {
    server.before(
        context -> {
          ContextProvider.set(context);
          EntityManager em = persistenceManager.getEntityManagerFactory().createEntityManager();
          em.getTransaction().begin();
          context.attribute("em", em);
        });
    server.after(
        ctx -> {
          EntityManager em = ctx.attribute("em");
          boolean existsEm = em != null && em.isOpen();
          try {
            if (!existsEm || !em.getTransaction().isActive()) {
              return;
            }

            if (ctx.status().getCode() >= 400) {
              em.getTransaction().rollback();
            } else {
              em.getTransaction().commit();
            }

          } finally {
            if (existsEm) em.close();
            ContextProvider.clear();
          }
        });
  }
}
