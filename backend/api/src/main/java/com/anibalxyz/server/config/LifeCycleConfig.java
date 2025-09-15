package com.anibalxyz.server.config;

import com.anibalxyz.persistence.PersistenceManager;
import com.anibalxyz.server.context.ContextProvider;
import io.javalin.Javalin;
import jakarta.persistence.EntityManager;

public class LifeCycleConfig implements ServerConfig {

  private final Javalin server;
  private final PersistenceManager persistenceManager;

  public LifeCycleConfig(Javalin server, PersistenceManager persistenceManager) {
    this.server = server;
    this.persistenceManager = persistenceManager;
  }

  private void setEntityManagerLifecycle() {
    server.before(
        ctx -> {
          ContextProvider.set(ctx);
          EntityManager em = persistenceManager.emf().createEntityManager();
          em.getTransaction().begin();
          ctx.attribute("em", em);
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

  @Override
  public void apply() {
    setEntityManagerLifecycle();
  }
}
