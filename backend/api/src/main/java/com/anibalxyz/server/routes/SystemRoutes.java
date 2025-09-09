package com.anibalxyz.server.routes;

import com.anibalxyz.persistence.PersistenceManager;
import io.javalin.Javalin;
import jakarta.persistence.EntityManager;
import java.util.Map;

public class SystemRoutes {
  private final Javalin app;
  private final PersistenceManager persistenceManager;

  public SystemRoutes(Javalin app, PersistenceManager persistenceManager) {
    this.app = app;
    this.persistenceManager = persistenceManager;
  }

  public void register() {
    app.get(
        "/health",
        ctx -> {
          boolean dbIsConnected;

          try (EntityManager em =
              persistenceManager.getEntityManagerFactory().createEntityManager()) {
            em.createNativeQuery("SELECT 1").getSingleResult();
            dbIsConnected = true;
          } catch (Exception e) {
            dbIsConnected = false;
          }

          ctx.json(Map.of("status", dbIsConnected));
        });
  }
}
