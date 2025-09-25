package com.anibalxyz.server.routes;

import com.anibalxyz.persistence.PersistenceManager;
import io.javalin.Javalin;
import jakarta.persistence.EntityManager;
import java.util.Map;

public class SystemRoutes extends RouteRegistry {

  private final PersistenceManager persistenceManager;

  public SystemRoutes(Javalin server, PersistenceManager persistenceManager) {
    super(server);
    this.persistenceManager = persistenceManager;
  }

  @Override
  public void register() {
    server.get(
        "/health",
        ctx -> {
          boolean dbIsConnected;

          try (EntityManager em = persistenceManager.emf().createEntityManager()) {
            em.createNativeQuery("SELECT 1").getSingleResult();
            dbIsConnected = true;
          } catch (Exception e) {
            dbIsConnected = false;
          }

          ctx.json(Map.of("status", dbIsConnected));
        });
  }
}
