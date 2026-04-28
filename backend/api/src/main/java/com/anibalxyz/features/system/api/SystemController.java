package com.anibalxyz.features.system.api;

import com.anibalxyz.features.system.api.out.HealthResponse;
import com.anibalxyz.persistence.PersistenceManager;
import io.javalin.http.Context;
import jakarta.persistence.EntityManager;

public class SystemController implements SystemApi {

  private final PersistenceManager persistenceManager;

  public SystemController(PersistenceManager persistenceManager) {
    this.persistenceManager = persistenceManager;
  }

  /** {@inheritDoc} */
  @Override
  public void healthCheck(Context ctx) {
    boolean dbIsConnected;
    try (EntityManager em = persistenceManager.emf().createEntityManager()) {
      em.createNativeQuery("SELECT 1").getSingleResult();
      dbIsConnected = true;
    } catch (Exception e) {
      dbIsConnected = false;
    }
    ctx.json(new HealthResponse(dbIsConnected));
  }
}
